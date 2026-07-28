package com.familytree.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Signup submission for the new UserAccount/VerificationRequest pipeline
 * (docs/05-auth-and-verification.md). AD is the required canonical date
 * input; the BS equivalent is derived server-side (see SignupService),
 * not trusted from the client, since the frontend's live conversion is a
 * UX convenience, not a validated input.
 *
 * Validation here is a server-side safety net -- the Next.js signup form
 * is expected to do the primary, localized UX validation.
 */
public class SignupRequestDto {

    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    private String email;

    @NotBlank(message = "Full name is required.")
    @Size(max = 255, message = "Full name must be 255 characters or fewer.")
    private String fullName;

    @NotNull(message = "Date of birth is required.")
    @Past(message = "Date of birth must be in the past.")
    private LocalDate dobAd;

    @NotBlank(message = "Father's full name is required.")
    @Size(max = 255, message = "Father's name must be 255 characters or fewer.")
    private String fatherName;

    @NotBlank(message = "Grandfather's full name is required.")
    @Size(max = 255, message = "Grandfather's name must be 255 characters or fewer.")
    private String grandfatherName;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters.")
    private String password;

    @NotBlank(message = "Please confirm your password.")
    private String confirmPassword;

    @NotBlank(message = "Preferred language is required.")
    private String preferredLanguage;

    @AssertTrue(message = "You must agree to the privacy policy and terms.")
    private boolean agreedToTerms;

    // Optional, admin-configurable fields -- see docs/05.
    private String motherName;
    private String placeOfBirth;
    private String ancestralVillage;
    private String familyBranch;
    private String knownRelativeName;
    private String invitationCode;

    @Size(max = 2000, message = "Note must be 2000 characters or fewer.")
    private String applicantNote;

    public boolean passwordsMatch() {
        return password != null && password.equals(confirmPassword);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getDobAd() {
        return dobAd;
    }

    public void setDobAd(LocalDate dobAd) {
        this.dobAd = dobAd;
    }

    public String getFatherName() {
        return fatherName;
    }

    public void setFatherName(String fatherName) {
        this.fatherName = fatherName;
    }

    public String getGrandfatherName() {
        return grandfatherName;
    }

    public void setGrandfatherName(String grandfatherName) {
        this.grandfatherName = grandfatherName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public boolean isAgreedToTerms() {
        return agreedToTerms;
    }

    public void setAgreedToTerms(boolean agreedToTerms) {
        this.agreedToTerms = agreedToTerms;
    }

    public String getMotherName() {
        return motherName;
    }

    public void setMotherName(String motherName) {
        this.motherName = motherName;
    }

    public String getPlaceOfBirth() {
        return placeOfBirth;
    }

    public void setPlaceOfBirth(String placeOfBirth) {
        this.placeOfBirth = placeOfBirth;
    }

    public String getAncestralVillage() {
        return ancestralVillage;
    }

    public void setAncestralVillage(String ancestralVillage) {
        this.ancestralVillage = ancestralVillage;
    }

    public String getFamilyBranch() {
        return familyBranch;
    }

    public void setFamilyBranch(String familyBranch) {
        this.familyBranch = familyBranch;
    }

    public String getKnownRelativeName() {
        return knownRelativeName;
    }

    public void setKnownRelativeName(String knownRelativeName) {
        this.knownRelativeName = knownRelativeName;
    }

    public String getInvitationCode() {
        return invitationCode;
    }

    public void setInvitationCode(String invitationCode) {
        this.invitationCode = invitationCode;
    }

    public String getApplicantNote() {
        return applicantNote;
    }

    public void setApplicantNote(String applicantNote) {
        this.applicantNote = applicantNote;
    }
}
