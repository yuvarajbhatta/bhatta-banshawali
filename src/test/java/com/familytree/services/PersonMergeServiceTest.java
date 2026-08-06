package com.familytree.services;

import com.familytree.dto.MergeResultDto;
import com.familytree.entity.Person;
import com.familytree.entity.PersonCorrectionRequest;
import com.familytree.entity.Relationship;
import com.familytree.entity.RelationshipType;
import com.familytree.entity.UserPersonLink;
import com.familytree.entity.UserPersonLinkStatus;
import com.familytree.repository.PersonCorrectionRequestRepository;
import com.familytree.repository.PersonPhotoRepository;
import com.familytree.repository.PersonRepository;
import com.familytree.repository.RelationshipRepository;
import com.familytree.repository.UserPersonLinkRepository;
import com.familytree.web.PersonDisplayHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonMergeServiceTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private RelationshipRepository relationshipRepository;

    @Mock
    private UserPersonLinkRepository userPersonLinkRepository;

    @Mock
    private PersonCorrectionRequestRepository personCorrectionRequestRepository;

    @Mock
    private PersonPhotoRepository personPhotoRepository;

    @Mock
    private AuditLogService auditLogService;

    private final PersonDisplayHelper personDisplay = new PersonDisplayHelper();

    private PersonMergeService service;

    @BeforeEach
    void setUp() {
        service = new PersonMergeService(personRepository, relationshipRepository, userPersonLinkRepository,
                personCorrectionRequestRepository, personPhotoRepository, auditLogService, personDisplay);
        lenient().when(userPersonLinkRepository.findByPersonId(any())).thenReturn(List.of());
        lenient().when(personCorrectionRequestRepository.findByPersonId(any())).thenReturn(List.of());
        lenient().when(personPhotoRepository.findByPersonIdOrderByUploadedAtDesc(any())).thenReturn(List.of());
        lenient().when(relationshipRepository.findByPerson(any())).thenReturn(List.of());
        lenient().when(relationshipRepository.findByRelatedPerson(any())).thenReturn(List.of());
        lenient().when(relationshipRepository.findAll()).thenReturn(List.of());
    }

    @Test
    void rejectsMergingAPersonWithThemselves() {
        assertThatThrownBy(() -> service.merge(1L, 1L, "admin"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(personRepository);
    }

    @Test
    void throws404WhenSurvivorMissing() {
        when(personRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.merge(1L, 2L, "admin"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void rejectsDirectlyRelatedPair() {
        Person survivor = person(1L);
        Person loser = person(2L);
        when(personRepository.findById(1L)).thenReturn(Optional.of(survivor));
        when(personRepository.findById(2L)).thenReturn(Optional.of(loser));

        Relationship directLink = new Relationship();
        directLink.setPerson(survivor);
        directLink.setRelatedPerson(loser);
        directLink.setRelationshipType(RelationshipType.SPOUSE);
        when(relationshipRepository.findByPerson(survivor)).thenReturn(List.of(directLink));

        assertThatThrownBy(() -> service.merge(1L, 2L, "admin"))
                .isInstanceOf(PersonMergeConflictException.class)
                .hasMessageContaining("directly related");
    }

    @Test
    void rejectsWhenBothSidesHaveAVerifiedAccountLink() {
        Person survivor = person(1L);
        Person loser = person(2L);
        when(personRepository.findById(1L)).thenReturn(Optional.of(survivor));
        when(personRepository.findById(2L)).thenReturn(Optional.of(loser));
        when(userPersonLinkRepository.findByPersonId(1L)).thenReturn(List.of(verifiedLink()));
        when(userPersonLinkRepository.findByPersonId(2L)).thenReturn(List.of(verifiedLink()));

        assertThatThrownBy(() -> service.merge(1L, 2L, "admin"))
                .isInstanceOf(PersonMergeConflictException.class)
                .hasMessageContaining("verified user account");
    }

    @Test
    void happyPathRepointsEverythingAndDeletesTheLoser() {
        Person survivor = person(1L);
        Person loser = person(2L);
        Person other = person(3L);
        Person another = person(4L);
        Person someoneElse = person(5L);
        when(personRepository.findById(1L)).thenReturn(Optional.of(survivor));
        when(personRepository.findById(2L)).thenReturn(Optional.of(loser));

        // Survivor already has (survivor -> other, CHILD); loser has the same
        // edge to `other`, which must be dropped as a duplicate rather than
        // re-pointed (it would collide with the unique constraint), while
        // loser's other edges have no collision and get re-pointed normally.
        Relationship survivorChild = relationship(survivor, other, RelationshipType.CHILD);
        Relationship loserChildDuplicate = relationship(loser, other, RelationshipType.CHILD);
        Relationship loserChildNew = relationship(loser, another, RelationshipType.CHILD);
        Relationship someoneElseIsFatherOfLoser = relationship(someoneElse, loser, RelationshipType.FATHER);

        when(relationshipRepository.findByPerson(loser)).thenReturn(List.of(loserChildDuplicate, loserChildNew));
        when(relationshipRepository.findByRelatedPerson(loser)).thenReturn(List.of(someoneElseIsFatherOfLoser));
        when(relationshipRepository.findAll()).thenReturn(
                List.of(survivorChild, loserChildDuplicate, loserChildNew, someoneElseIsFatherOfLoser));

        UserPersonLink loserAccountLink = verifiedLink();
        when(userPersonLinkRepository.findByPersonId(2L)).thenReturn(List.of(loserAccountLink));

        PersonCorrectionRequest correctionRequest = new PersonCorrectionRequest();
        correctionRequest.setPerson(loser);
        when(personCorrectionRequestRepository.findByPersonId(2L)).thenReturn(List.of(correctionRequest));

        com.familytree.entity.PersonPhoto loserPhoto = new com.familytree.entity.PersonPhoto();
        loserPhoto.setPerson(loser);
        when(personPhotoRepository.findByPersonIdOrderByUploadedAtDesc(2L)).thenReturn(List.of(loserPhoto));

        MergeResultDto result = service.merge(1L, 2L, "admin");

        assertThat(result.survivorId()).isEqualTo(1L);
        assertThat(result.relationshipsRepointed()).isEqualTo(2);
        assertThat(result.relationshipsDroppedAsDuplicate()).isEqualTo(1);
        assertThat(result.userLinksRepointed()).isEqualTo(1);
        assertThat(result.correctionRequestsRepointed()).isEqualTo(1);
        assertThat(result.photosRepointed()).isEqualTo(1);
        assertThat(loserPhoto.getPerson()).isEqualTo(survivor);
        verify(personPhotoRepository).save(loserPhoto);

        assertThat(loserChildNew.getPerson()).isEqualTo(survivor);
        assertThat(someoneElseIsFatherOfLoser.getRelatedPerson()).isEqualTo(survivor);
        verify(relationshipRepository).save(loserChildNew);
        verify(relationshipRepository).save(someoneElseIsFatherOfLoser);
        verify(relationshipRepository).delete(loserChildDuplicate);
        verify(relationshipRepository, never()).save(loserChildDuplicate);

        assertThat(loserAccountLink.getPerson()).isEqualTo(survivor);
        verify(userPersonLinkRepository).save(loserAccountLink);

        assertThat(correctionRequest.getPerson()).isEqualTo(survivor);
        verify(personCorrectionRequestRepository).save(correctionRequest);

        verify(personRepository).delete(loser);
        verify(auditLogService).record(eq(AuditLogService.ACTION_PERSON_MERGED), eq(AuditLogService.ENTITY_PERSON),
                eq(1L), any(String.class), eq("admin"));
    }

    private Person person(Long id) {
        Person person = new Person();
        person.setId(id);
        person.setFirstName("Person" + id);
        person.setLastName("Bhatta");
        return person;
    }

    private Relationship relationship(Person person, Person relatedPerson, RelationshipType type) {
        Relationship relationship = new Relationship();
        relationship.setPerson(person);
        relationship.setRelatedPerson(relatedPerson);
        relationship.setRelationshipType(type);
        return relationship;
    }

    private UserPersonLink verifiedLink() {
        UserPersonLink link = new UserPersonLink();
        link.setLinkStatus(UserPersonLinkStatus.VERIFIED);
        return link;
    }
}
