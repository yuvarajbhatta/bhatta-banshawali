package com.familytree.services;

import com.familytree.entity.Person;
import com.familytree.entity.PersonPhoto;
import com.familytree.entity.UserAccount;
import com.familytree.entity.UserPersonLink;
import com.familytree.entity.UserPersonLinkStatus;
import com.familytree.repository.PersonPhotoRepository;
import com.familytree.repository.PersonRepository;
import com.familytree.repository.UserAccountRepository;
import com.familytree.repository.UserPersonLinkRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Picture Album (docs/06-ui-ux-specification.md dashboard). Ships
 * auto-published (no admin review queue) -- ImageReencodeService is the
 * only real safety net for that, so authorization here stays deliberately
 * narrow: a member can only add a photo of themselves or their immediate
 * family (father/mother/spouse/children/siblings), same circle as "Your
 * Family". Admins bypass that restriction. Viewing an existing photo has
 * no such restriction, same visibility tier as names/relationships
 * generally.
 */
@Service
public class PersonPhotoService {

    static final int MAX_PHOTOS_PER_PERSON = 20;

    private final PersonPhotoRepository personPhotoRepository;
    private final PersonRepository personRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserPersonLinkRepository userPersonLinkRepository;
    private final RelationshipService relationshipService;
    private final ImageReencodeService imageReencodeService;
    private final AuditLogService auditLogService;
    private final PhotoStorageService photoStorageService;

    public PersonPhotoService(PersonPhotoRepository personPhotoRepository,
                              PersonRepository personRepository,
                              UserAccountRepository userAccountRepository,
                              UserPersonLinkRepository userPersonLinkRepository,
                              RelationshipService relationshipService,
                              ImageReencodeService imageReencodeService,
                              AuditLogService auditLogService,
                              PhotoStorageService photoStorageService) {
        this.personPhotoRepository = personPhotoRepository;
        this.personRepository = personRepository;
        this.userAccountRepository = userAccountRepository;
        this.userPersonLinkRepository = userPersonLinkRepository;
        this.relationshipService = relationshipService;
        this.imageReencodeService = imageReencodeService;
        this.auditLogService = auditLogService;
        this.photoStorageService = photoStorageService;
    }

    public List<PersonPhoto> list(Long personId) {
        return personPhotoRepository.findByPersonIdOrderByUploadedAtDesc(personId);
    }

    @Transactional
    public PersonPhoto upload(Long personId, MultipartFile file, String caption, String uploaderEmail, boolean isUploaderAdmin) {
        Person target = personRepository.findById(personId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found with id: " + personId));
        UserAccount uploaderAccount = userAccountRepository.findByEmail(uploaderEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No member profile for this account."));

        if (!isUploaderAdmin) {
            Person uploaderPerson = findVerifiedPerson(uploaderAccount)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Your account isn't linked to a family record yet."));
            if (!isSelfOrImmediateFamily(uploaderPerson, personId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You can only add photos of yourself or your immediate family.");
            }
        }

        if (personPhotoRepository.countByPersonId(personId) >= MAX_PHOTOS_PER_PERSON) {
            throw new IllegalArgumentException("This person already has the maximum of " + MAX_PHOTOS_PER_PERSON + " photos.");
        }

        byte[] original;
        try {
            original = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the uploaded file.");
        }
        byte[] reencoded = imageReencodeService.reencode(original);
        String storageKey = photoStorageService.store(reencoded);

        PersonPhoto photo = new PersonPhoto();
        photo.setPerson(target);
        photo.setUploadedBy(uploaderAccount);
        photo.setStorageKey(storageKey);
        photo.setMimeType(ImageReencodeService.OUTPUT_MIME_TYPE);
        photo.setFileSizeBytes(reencoded.length);
        photo.setCaption(blankToNull(caption));
        photo.setUploadedAt(LocalDateTime.now());
        return personPhotoRepository.save(photo);
    }

    @Transactional
    public void delete(Long personId, Long photoId, String actorEmail, boolean isActorAdmin) {
        PersonPhoto photo = getOrThrow(personId, photoId);
        UserAccount actorAccount = userAccountRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No member profile for this account."));

        boolean isOwnUpload = photo.getUploadedBy().getId().equals(actorAccount.getId());
        if (!isActorAdmin && !isOwnUpload) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only remove photos you uploaded.");
        }

        photoStorageService.delete(photo.getStorageKey());
        personPhotoRepository.deleteById(photoId);

        // Only a genuine moderation action (an admin removing someone
        // else's upload) is audit-logged -- an uploader deleting their own
        // photo is ordinary self-service, same as PersonCorrectionService's
        // submit() not being logged while its admin-decided approve/reject
        // are.
        if (isActorAdmin && !isOwnUpload) {
            auditLogService.record(AuditLogService.ACTION_PHOTO_DELETED, AuditLogService.ENTITY_PERSON_PHOTO, photoId,
                    "Removed a photo of person #" + personId + " uploaded by " + photo.getUploadedBy().getEmail(), actorEmail);
        }
    }

    public record StoredPhotoFile(byte[] bytes, String mimeType) {
    }

    public StoredPhotoFile readFile(Long personId, Long photoId) {
        PersonPhoto photo = getOrThrow(personId, photoId);
        return new StoredPhotoFile(photoStorageService.read(photo.getStorageKey()), photo.getMimeType());
    }

    private PersonPhoto getOrThrow(Long personId, Long photoId) {
        return personPhotoRepository.findById(photoId)
                .filter(photo -> photo.getPerson().getId().equals(personId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found with id: " + photoId));
    }

    private Optional<Person> findVerifiedPerson(UserAccount account) {
        return userPersonLinkRepository.findByUserAccountId(account.getId()).stream()
                .filter(link -> link.getLinkStatus() == UserPersonLinkStatus.VERIFIED)
                .map(UserPersonLink::getPerson)
                .findFirst();
    }

    private boolean isSelfOrImmediateFamily(Person uploaderPerson, Long targetPersonId) {
        if (uploaderPerson.getId().equals(targetPersonId)) {
            return true;
        }
        Set<Long> immediateFamilyIds = new HashSet<>();
        relationshipService.getFatherForPerson(uploaderPerson).ifPresent(p -> immediateFamilyIds.add(p.getId()));
        relationshipService.getMotherForPerson(uploaderPerson).ifPresent(p -> immediateFamilyIds.add(p.getId()));
        relationshipService.getSpousesForPerson(uploaderPerson).forEach(p -> immediateFamilyIds.add(p.getId()));
        relationshipService.getChildrenForPerson(uploaderPerson).forEach(p -> immediateFamilyIds.add(p.getId()));
        relationshipService.getSiblingsForPerson(uploaderPerson).forEach(p -> immediateFamilyIds.add(p.getId()));
        return immediateFamilyIds.contains(targetPersonId);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
