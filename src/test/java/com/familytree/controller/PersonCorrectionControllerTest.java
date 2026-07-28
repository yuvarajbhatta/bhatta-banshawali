package com.familytree.controller;

import com.familytree.dto.CorrectionRequestDto;
import com.familytree.dto.CorrectionResponseDto;
import com.familytree.entity.CorrectablePersonField;
import com.familytree.entity.PersonCorrectionRequest;
import com.familytree.services.PersonCorrectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonCorrectionControllerTest {

    @Mock
    private PersonCorrectionService personCorrectionService;

    @Mock
    private Authentication authentication;

    private PersonCorrectionController controller() {
        return new PersonCorrectionController(personCorrectionService);
    }

    @Test
    void submitDelegatesToServiceWithSubmitterEmailAndReturnsSubmittedStatus() {
        when(authentication.getName()).thenReturn("member@example.com");
        CorrectionRequestDto dto = new CorrectionRequestDto();
        dto.setField(CorrectablePersonField.NICKNAME);
        dto.setProposedValue("New Nickname");
        dto.setReason("Family calls him this now");
        when(personCorrectionService.submit(1L, CorrectablePersonField.NICKNAME, "New Nickname",
                "Family calls him this now", "member@example.com"))
                .thenReturn(new PersonCorrectionRequest());

        CorrectionResponseDto response = controller().submit(1L, dto, authentication);

        assertThat(response.status()).isEqualTo("SUBMITTED");
        verify(personCorrectionService).submit(1L, CorrectablePersonField.NICKNAME, "New Nickname",
                "Family calls him this now", "member@example.com");
    }
}
