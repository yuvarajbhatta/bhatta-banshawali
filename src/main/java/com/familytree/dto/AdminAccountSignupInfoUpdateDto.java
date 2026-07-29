package com.familytree.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * Admin correction of an applicant's submitted identity info (the most
 * recent VerificationRequest for an account) -- e.g. a typo or a
 * deliberately fake name at signup, or disambiguating from another
 * Person with the same name before linking. See
 * UserAccountAdminService#updateSignupInfo.
 */
public class AdminAccountSignupInfoUpdateDto {

    @NotBlank(message = "Full name is required.")
    private String fullName;

    @NotBlank(message = "Father's name is required.")
    private String fatherName;

    private String motherName;

    @NotBlank(message = "Grandfather's name is required.")
    private String grandfatherName;

    private LocalDate dobAd;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getFatherName() {
        return fatherName;
    }

    public void setFatherName(String fatherName) {
        this.fatherName = fatherName;
    }

    public String getMotherName() {
        return motherName;
    }

    public void setMotherName(String motherName) {
        this.motherName = motherName;
    }

    public String getGrandfatherName() {
        return grandfatherName;
    }

    public void setGrandfatherName(String grandfatherName) {
        this.grandfatherName = grandfatherName;
    }

    public LocalDate getDobAd() {
        return dobAd;
    }

    public void setDobAd(LocalDate dobAd) {
        this.dobAd = dobAd;
    }
}
