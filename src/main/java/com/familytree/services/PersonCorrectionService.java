package com.familytree.services;

import com.familytree.entity.CorrectablePersonField;
import com.familytree.entity.CorrectionRequestStatus;
import com.familytree.entity.Person;
import com.familytree.entity.PersonCorrectionRequest;
import com.familytree.entity.UserAccount;
import com.familytree.repository.PersonCorrectionRequestRepository;
import com.familytree.repository.PersonRepository;
import com.familytree.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Member-submitted corrections to Person fields, queued for admin
 * review (docs/08 Phase 4). See PersonCorrectionRequest for how this
 * relates to the full ChangeRequest design in docs/04.
 */
@Service
public class PersonCorrectionService {

    private final PersonCorrectionRequestRepository correctionRequestRepository;
    private final PersonRepository personRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditLogService auditLogService;

    public PersonCorrectionService(PersonCorrectionRequestRepository correctionRequestRepository,
                                   PersonRepository personRepository,
                                   UserAccountRepository userAccountRepository,
                                   AuditLogService auditLogService) {
        this.correctionRequestRepository = correctionRequestRepository;
        this.personRepository = personRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public PersonCorrectionRequest submit(Long personId, CorrectablePersonField field, String proposedValue,
                                          String reason, String submitterEmail) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found with id: " + personId));
        UserAccount submitter = userAccountRepository.findByEmail(submitterEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No member profile for this account."));

        PersonCorrectionRequest request = new PersonCorrectionRequest();
        request.setPerson(person);
        request.setSubmittedBy(submitter);
        request.setField(field);
        request.setCurrentValueSnapshot(readField(person, field));
        request.setProposedValue(proposedValue);
        request.setReason(reason);
        request.setStatus(CorrectionRequestStatus.PENDING);
        request.setSubmittedAt(LocalDateTime.now());

        return correctionRequestRepository.save(request);
    }

    @Transactional
    public void approve(Long requestId, String reviewerUsername, String decisionNote) {
        PersonCorrectionRequest request = getOrThrow(requestId);
        applyField(request.getPerson(), request.getField(), request.getProposedValue());
        personRepository.save(request.getPerson());
        markReviewed(request, CorrectionRequestStatus.APPROVED, reviewerUsername, decisionNote);

        auditLogService.record(AuditLogService.ACTION_CORRECTION_APPROVED, AuditLogService.ENTITY_CORRECTION_REQUEST,
                requestId, "Approved correction to " + request.getField() + " for person #" + request.getPerson().getId(),
                reviewerUsername);
    }

    @Transactional
    public void reject(Long requestId, String reviewerUsername, String decisionNote) {
        PersonCorrectionRequest request = getOrThrow(requestId);
        markReviewed(request, CorrectionRequestStatus.REJECTED, reviewerUsername, decisionNote);

        auditLogService.record(AuditLogService.ACTION_CORRECTION_REJECTED, AuditLogService.ENTITY_CORRECTION_REQUEST,
                requestId, "Rejected correction to " + request.getField() + " for person #" + request.getPerson().getId(),
                reviewerUsername);
    }

    private void markReviewed(PersonCorrectionRequest request, CorrectionRequestStatus status,
                              String reviewerUsername, String decisionNote) {
        request.setStatus(status);
        request.setReviewedByUsername(reviewerUsername);
        request.setReviewedAt(LocalDateTime.now());
        request.setDecisionNote(decisionNote);
        correctionRequestRepository.save(request);
    }

    private PersonCorrectionRequest getOrThrow(Long id) {
        return correctionRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Correction request not found with id: " + id));
    }

    private String readField(Person person, CorrectablePersonField field) {
        return switch (field) {
            case FIRST_NAME -> person.getFirstName();
            case MIDDLE_NAME -> person.getMiddleName();
            case LAST_NAME -> person.getLastName();
            case FIRST_NAME_NEPALI -> person.getFirstNameNepali();
            case MIDDLE_NAME_NEPALI -> person.getMiddleNameNepali();
            case LAST_NAME_NEPALI -> person.getLastNameNepali();
            case NICKNAME -> person.getNickname();
            case GENDER -> person.getGender();
            case BIRTH_DATE -> person.getBirthDate() == null ? null : person.getBirthDate().toString();
            case DEATH_DATE -> person.getDeathDate() == null ? null : person.getDeathDate().toString();
            case BIRTH_PLACE -> person.getBirthPlace();
            case CURRENT_ADDRESS -> person.getCurrentAddress();
            case NOTES -> person.getNotes();
            case GENERATION_NUMBER -> person.getGenerationNumber() == null ? null : person.getGenerationNumber().toString();
        };
    }

    private void applyField(Person person, CorrectablePersonField field, String value) {
        switch (field) {
            case FIRST_NAME -> person.setFirstName(value);
            case MIDDLE_NAME -> person.setMiddleName(value);
            case LAST_NAME -> person.setLastName(value);
            case FIRST_NAME_NEPALI -> person.setFirstNameNepali(value);
            case MIDDLE_NAME_NEPALI -> person.setMiddleNameNepali(value);
            case LAST_NAME_NEPALI -> person.setLastNameNepali(value);
            case NICKNAME -> person.setNickname(value);
            case GENDER -> person.setGender(value);
            case BIRTH_DATE -> person.setBirthDate(LocalDate.parse(value));
            case DEATH_DATE -> person.setDeathDate(LocalDate.parse(value));
            case BIRTH_PLACE -> person.setBirthPlace(value);
            case CURRENT_ADDRESS -> person.setCurrentAddress(value);
            case NOTES -> person.setNotes(value);
            case GENERATION_NUMBER -> person.setGenerationNumber(Integer.parseInt(value));
        }
    }
}
