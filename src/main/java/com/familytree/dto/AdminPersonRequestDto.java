package com.familytree.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Create/update body for POST|PUT /api/v1/admin/persons -- same field set and limits as the Person entity. */
public class AdminPersonRequestDto {

    private Integer generationNumber;

    @NotBlank(message = "First name is required.")
    @Size(max = 100, message = "First name must be 100 characters or fewer.")
    private String firstName;

    @Size(max = 100, message = "First name (Nepali) must be 100 characters or fewer.")
    private String firstNameNepali;

    private String middleName;

    @Size(max = 100, message = "Middle name (Nepali) must be 100 characters or fewer.")
    private String middleNameNepali;

    @NotBlank(message = "Last name is required.")
    @Size(max = 100, message = "Last name must be 100 characters or fewer.")
    private String lastName;

    @Size(max = 100, message = "Last name (Nepali) must be 100 characters or fewer.")
    private String lastNameNepali;

    @Size(max = 100, message = "Nickname must be 100 characters or fewer.")
    private String nickname;

    private String gender;
    private LocalDate birthDate;
    private LocalDate deathDate;

    @Size(max = 255, message = "Photo path must be 255 characters or fewer.")
    private String photoPath;

    @Size(max = 255, message = "Birth place must be 255 characters or fewer.")
    private String birthPlace;

    @Size(max = 500, message = "Current address must be 500 characters or fewer.")
    private String currentAddress;

    @Size(max = 4000, message = "Notes must be 4000 characters or fewer.")
    private String notes;

    public Integer getGenerationNumber() {
        return generationNumber;
    }

    public void setGenerationNumber(Integer generationNumber) {
        this.generationNumber = generationNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getFirstNameNepali() {
        return firstNameNepali;
    }

    public void setFirstNameNepali(String firstNameNepali) {
        this.firstNameNepali = firstNameNepali;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getMiddleNameNepali() {
        return middleNameNepali;
    }

    public void setMiddleNameNepali(String middleNameNepali) {
        this.middleNameNepali = middleNameNepali;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getLastNameNepali() {
        return lastNameNepali;
    }

    public void setLastNameNepali(String lastNameNepali) {
        this.lastNameNepali = lastNameNepali;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public LocalDate getDeathDate() {
        return deathDate;
    }

    public void setDeathDate(LocalDate deathDate) {
        this.deathDate = deathDate;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    public void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

    public String getCurrentAddress() {
        return currentAddress;
    }

    public void setCurrentAddress(String currentAddress) {
        this.currentAddress = currentAddress;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
