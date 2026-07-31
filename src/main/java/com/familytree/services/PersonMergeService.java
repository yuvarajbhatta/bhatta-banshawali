package com.familytree.services;

import com.familytree.dto.MergeResultDto;
import com.familytree.entity.Person;
import com.familytree.entity.PersonCorrectionRequest;
import com.familytree.entity.Relationship;
import com.familytree.entity.RelationshipType;
import com.familytree.entity.UserPersonLink;
import com.familytree.entity.UserPersonLinkStatus;
import com.familytree.repository.PersonCorrectionRequestRepository;
import com.familytree.repository.PersonRepository;
import com.familytree.repository.RelationshipRepository;
import com.familytree.repository.UserPersonLinkRepository;
import com.familytree.web.PersonDisplayHelper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Guided (never automatic) person merge (docs/08 Phase 6). Re-points
 * Relationship, UserPersonLink, and PersonCorrectionRequest rows from the
 * losing record to the surviving one, then deletes the loser. Translates
 * docs/04-data-model.md's merge-integrity intent (re-point SourceCitation/
 * ChangeRequest/AuditEvent references) to what actually exists in this
 * schema -- SourceCitation and a generic ChangeRequest/AuditEvent entity
 * don't exist in code, only PersonCorrectionRequest and AuditLogEntry do,
 * and AuditLogEntry.entityId isn't a real FK so old entries naturally keep
 * referencing the old id without needing any re-pointing.
 *
 * Deliberately out of scope: no post-merge cycle re-check. Re-pointing a
 * genuine duplicate's edges essentially can't introduce a new ancestry
 * cycle -- the unique-constraint-collision handling below already catches
 * the case where it plausibly could. DataQualityService's cycle report
 * catches it on next view in the rare case one does appear anyway.
 */
@Service
public class PersonMergeService {

    private final PersonRepository personRepository;
    private final RelationshipRepository relationshipRepository;
    private final UserPersonLinkRepository userPersonLinkRepository;
    private final PersonCorrectionRequestRepository personCorrectionRequestRepository;
    private final AuditLogService auditLogService;
    private final PersonDisplayHelper personDisplay;

    public PersonMergeService(PersonRepository personRepository,
                              RelationshipRepository relationshipRepository,
                              UserPersonLinkRepository userPersonLinkRepository,
                              PersonCorrectionRequestRepository personCorrectionRequestRepository,
                              AuditLogService auditLogService,
                              PersonDisplayHelper personDisplay) {
        this.personRepository = personRepository;
        this.relationshipRepository = relationshipRepository;
        this.userPersonLinkRepository = userPersonLinkRepository;
        this.personCorrectionRequestRepository = personCorrectionRequestRepository;
        this.auditLogService = auditLogService;
        this.personDisplay = personDisplay;
    }

    /**
     * @throws IllegalArgumentException if survivorId equals loserId
     * @throws PersonMergeConflictException if the two records are directly related, or
     *          both are linked to a verified user account
     */
    @Transactional
    public MergeResultDto merge(Long survivorId, Long loserId, String actorUsername) {
        if (survivorId.equals(loserId)) {
            throw new IllegalArgumentException("Cannot merge a person with themselves.");
        }

        Person survivor = getPersonOrThrow(survivorId);
        Person loser = getPersonOrThrow(loserId);

        // Not redundant with DuplicateCandidateService's own exclusion of
        // directly-related pairs: the candidate list the admin is looking
        // at can go stale (someone adds a relationship between the two
        // after it was fetched, before merge is clicked), so this check is
        // load-bearing here too, not just defense-in-depth.
        if (directlyRelated(survivor, loser)) {
            throw new PersonMergeConflictException(
                    "These records are directly related to each other -- resolve that relationship before merging.");
        }
        if (!verifiedLinksFor(survivorId).isEmpty() && !verifiedLinksFor(loserId).isEmpty()) {
            throw new PersonMergeConflictException(
                    "Both records are linked to a verified user account -- unlink one before merging.");
        }

        RelationshipRepointResult relationshipResult = repointRelationships(survivor, loser);
        int userLinksRepointed = repointUserPersonLinks(survivor, loser);
        int correctionRequestsRepointed = repointCorrectionRequests(survivor, loser);

        personRepository.delete(loser);

        auditLogService.record(AuditLogService.ACTION_PERSON_MERGED, AuditLogService.ENTITY_PERSON, survivor.getId(),
                "Merged #" + loser.getId() + " (" + personDisplay.englishFullName(loser) + ") into #"
                        + survivor.getId() + " (" + personDisplay.englishFullName(survivor) + "): "
                        + relationshipResult.repointed() + " relationships re-pointed, "
                        + relationshipResult.droppedAsDuplicate() + " dropped as duplicates, "
                        + userLinksRepointed + " account links re-pointed, "
                        + correctionRequestsRepointed + " correction requests re-pointed.",
                actorUsername);

        return new MergeResultDto(survivor.getId(), relationshipResult.repointed(),
                relationshipResult.droppedAsDuplicate(), userLinksRepointed, correctionRequestsRepointed);
    }

    private boolean directlyRelated(Person a, Person b) {
        boolean aToB = relationshipRepository.findByPerson(a).stream()
                .anyMatch(relationship -> relationship.getRelatedPerson().getId().equals(b.getId()));
        boolean bToA = relationshipRepository.findByPerson(b).stream()
                .anyMatch(relationship -> relationship.getRelatedPerson().getId().equals(a.getId()));
        return aToB || bToA;
    }

    private List<UserPersonLink> verifiedLinksFor(Long personId) {
        return userPersonLinkRepository.findByPersonId(personId).stream()
                .filter(link -> link.getLinkStatus() == UserPersonLinkStatus.VERIFIED)
                .toList();
    }

    private RelationshipRepointResult repointRelationships(Person survivor, Person loser) {
        List<Relationship> loserAsPerson = relationshipRepository.findByPerson(loser);
        List<Relationship> loserAsRelated = relationshipRepository.findByRelatedPerson(loser);

        Set<String> existingKeys = new HashSet<>();
        for (Relationship relationship : relationshipRepository.findAll()) {
            boolean involvesLoser = relationship.getPerson().getId().equals(loser.getId())
                    || relationship.getRelatedPerson().getId().equals(loser.getId());
            if (!involvesLoser) {
                existingKeys.add(relationshipKey(relationship.getPerson().getId(),
                        relationship.getRelatedPerson().getId(), relationship.getRelationshipType()));
            }
        }

        int repointed = 0;
        int dropped = 0;

        for (Relationship relationship : loserAsPerson) {
            String newKey = relationshipKey(survivor.getId(), relationship.getRelatedPerson().getId(), relationship.getRelationshipType());
            // The self-loop case (relatedPerson already == survivor) can only arise from
            // pre-existing corrupt data, since directlyRelated() already blocks the normal path.
            if (existingKeys.contains(newKey) || relationship.getRelatedPerson().getId().equals(survivor.getId())) {
                relationshipRepository.delete(relationship);
                dropped++;
            } else {
                relationship.setPerson(survivor);
                relationshipRepository.save(relationship);
                existingKeys.add(newKey);
                repointed++;
            }
        }

        for (Relationship relationship : loserAsRelated) {
            String newKey = relationshipKey(relationship.getPerson().getId(), survivor.getId(), relationship.getRelationshipType());
            if (existingKeys.contains(newKey) || relationship.getPerson().getId().equals(survivor.getId())) {
                relationshipRepository.delete(relationship);
                dropped++;
            } else {
                relationship.setRelatedPerson(survivor);
                relationshipRepository.save(relationship);
                existingKeys.add(newKey);
                repointed++;
            }
        }

        return new RelationshipRepointResult(repointed, dropped);
    }

    private String relationshipKey(Long personId, Long relatedPersonId, RelationshipType type) {
        return personId + ":" + relatedPersonId + ":" + type;
    }

    private int repointUserPersonLinks(Person survivor, Person loser) {
        List<UserPersonLink> links = userPersonLinkRepository.findByPersonId(loser.getId());
        for (UserPersonLink link : links) {
            link.setPerson(survivor);
            userPersonLinkRepository.save(link);
        }
        return links.size();
    }

    private int repointCorrectionRequests(Person survivor, Person loser) {
        List<PersonCorrectionRequest> requests = personCorrectionRequestRepository.findByPersonId(loser.getId());
        for (PersonCorrectionRequest request : requests) {
            request.setPerson(survivor);
            personCorrectionRequestRepository.save(request);
        }
        return requests.size();
    }

    private Person getPersonOrThrow(Long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found with id: " + id));
    }

    private record RelationshipRepointResult(int repointed, int droppedAsDuplicate) {
    }
}
