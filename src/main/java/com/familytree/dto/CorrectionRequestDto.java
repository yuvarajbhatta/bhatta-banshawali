package com.familytree.dto;

import com.familytree.entity.CorrectablePersonField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A member's submitted correction for one Person field -- see
 * PersonCorrectionRequest. proposedValue is always a plain string on
 * the wire (the client sends "1995-06-15" for a date field, "8" for
 * generation number, etc.); PersonCorrectionService parses it against
 * the real field type only when a correction is approved.
 */
public class CorrectionRequestDto {

    @NotNull(message = "Field is required.")
    private CorrectablePersonField field;

    @NotBlank(message = "Proposed value is required.")
    @Size(max = 1000, message = "Proposed value must be 1000 characters or fewer.")
    private String proposedValue;

    @NotBlank(message = "A reason is required.")
    @Size(max = 1000, message = "Reason must be 1000 characters or fewer.")
    private String reason;

    public CorrectablePersonField getField() {
        return field;
    }

    public void setField(CorrectablePersonField field) {
        this.field = field;
    }

    public String getProposedValue() {
        return proposedValue;
    }

    public void setProposedValue(String proposedValue) {
        this.proposedValue = proposedValue;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
