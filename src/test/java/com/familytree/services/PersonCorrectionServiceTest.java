package com.familytree.services;

import com.familytree.entity.CorrectablePersonField;
import com.familytree.entity.CorrectionRequestStatus;
import com.familytree.entity.Person;
import com.familytree.entity.PersonCorrectionRequest;
import com.familytree.entity.UserAccount;
import com.familytree.repository.PersonCorrectionRequestRepository;
import com.familytree.repository.PersonRepository;
import com.familytree.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonCorrectionServiceTest {

    @Mock
    private PersonCorrectionRequestRepository correctionRequestRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    private PersonCorrectionService service() {
        return new PersonCorrectionService(correctionRequestRepository, personRepository, userAccountRepository);
    }

    @Test
    void submitCapturesCurrentValueSnapshotAndQueuesAsPending() {
        Person person = new Person();
        person.setId(1L);
        person.setNickname("Old Nickname");
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));

        UserAccount submitter = new UserAccount();
        submitter.setEmail("member@example.com");
        when(userAccountRepository.findByEmail("member@example.com")).thenReturn(Optional.of(submitter));

        when(correctionRequestRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PersonCorrectionRequest request = service().submit(1L, CorrectablePersonField.NICKNAME, "New Nickname",
                "Family calls him this now", "member@example.com");

        assertThat(request.getPerson()).isEqualTo(person);
        assertThat(request.getSubmittedBy()).isEqualTo(submitter);
        assertThat(request.getField()).isEqualTo(CorrectablePersonField.NICKNAME);
        assertThat(request.getCurrentValueSnapshot()).isEqualTo("Old Nickname");
        assertThat(request.getProposedValue()).isEqualTo("New Nickname");
        assertThat(request.getReason()).isEqualTo("Family calls him this now");
        assertThat(request.getStatus()).isEqualTo(CorrectionRequestStatus.PENDING);
        assertThat(request.getSubmittedAt()).isNotNull();
    }

    @Test
    void submitThrows404WhenPersonDoesNotExist() {
        when(personRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().submit(999L, CorrectablePersonField.NICKNAME, "x", "y", "member@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Person not found");
    }

    @Test
    void submitThrows404WhenSubmitterHasNoUserAccount() {
        Person person = new Person();
        person.setId(1L);
        when(personRepository.findById(1L)).thenReturn(Optional.of(person));
        when(userAccountRepository.findByEmail("admin")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().submit(1L, CorrectablePersonField.NICKNAME, "x", "y", "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No member profile");
    }

    @Test
    void approveAppliesProposedValueAndMarksApproved() {
        Person person = new Person();
        person.setNickname("Old");
        PersonCorrectionRequest request = new PersonCorrectionRequest();
        request.setPerson(person);
        request.setField(CorrectablePersonField.NICKNAME);
        request.setProposedValue("New");
        when(correctionRequestRepository.findById(5L)).thenReturn(Optional.of(request));

        service().approve(5L, "admin", "looks right");

        assertThat(person.getNickname()).isEqualTo("New");
        assertThat(request.getStatus()).isEqualTo(CorrectionRequestStatus.APPROVED);
        assertThat(request.getReviewedByUsername()).isEqualTo("admin");
        assertThat(request.getDecisionNote()).isEqualTo("looks right");
        assertThat(request.getReviewedAt()).isNotNull();
        verify(personRepository).save(person);
        verify(correctionRequestRepository).save(request);
    }

    @Test
    void approveParsesBirthDateFieldAsALocalDate() {
        Person person = new Person();
        PersonCorrectionRequest request = new PersonCorrectionRequest();
        request.setPerson(person);
        request.setField(CorrectablePersonField.BIRTH_DATE);
        request.setProposedValue("1995-06-15");
        when(correctionRequestRepository.findById(6L)).thenReturn(Optional.of(request));

        service().approve(6L, "admin", null);

        assertThat(person.getBirthDate()).isEqualTo(LocalDate.of(1995, 6, 15));
    }

    @Test
    void approveParsesGenerationNumberFieldAsAnInteger() {
        Person person = new Person();
        PersonCorrectionRequest request = new PersonCorrectionRequest();
        request.setPerson(person);
        request.setField(CorrectablePersonField.GENERATION_NUMBER);
        request.setProposedValue("9");
        when(correctionRequestRepository.findById(7L)).thenReturn(Optional.of(request));

        service().approve(7L, "admin", null);

        assertThat(person.getGenerationNumber()).isEqualTo(9);
    }

    @Test
    void rejectDoesNotTouchThePersonRecord() {
        Person person = new Person();
        person.setNickname("Unchanged");
        PersonCorrectionRequest request = new PersonCorrectionRequest();
        request.setPerson(person);
        request.setField(CorrectablePersonField.NICKNAME);
        request.setProposedValue("Should not apply");
        when(correctionRequestRepository.findById(8L)).thenReturn(Optional.of(request));

        service().reject(8L, "admin", "not accurate");

        assertThat(person.getNickname()).isEqualTo("Unchanged");
        assertThat(request.getStatus()).isEqualTo(CorrectionRequestStatus.REJECTED);
        verify(personRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void approveThrowsWhenRequestNotFound() {
        when(correctionRequestRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().approve(404L, "admin", null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void reasonAndDecisionNoteAreCapturedConsistently() {
        Person person = new Person();
        PersonCorrectionRequest request = new PersonCorrectionRequest();
        request.setPerson(person);
        request.setField(CorrectablePersonField.NOTES);
        request.setProposedValue("Updated notes");
        when(correctionRequestRepository.findById(9L)).thenReturn(Optional.of(request));

        service().reject(9L, "reviewer-x", "insufficient evidence");

        ArgumentCaptor<PersonCorrectionRequest> captor = ArgumentCaptor.forClass(PersonCorrectionRequest.class);
        verify(correctionRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getReviewedByUsername()).isEqualTo("reviewer-x");
        assertThat(captor.getValue().getDecisionNote()).isEqualTo("insufficient evidence");
    }
}
