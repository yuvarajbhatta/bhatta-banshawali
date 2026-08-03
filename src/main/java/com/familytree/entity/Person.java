package com.familytree.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Entity
@Table(name = "persons")
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer generationNumber;

    @NotBlank(message = "{validation.person.firstName.required}")
    @Size(max =  100, message = "{validation.person.firstName.size}")
    private String firstName;
    @Size(max = 100, message = "{validation.person.firstName.size}")
    private String firstNameNepali;

    private String middleName;
    @Size(max = 100, message = "{validation.person.firstName.size}")
    private String middleNameNepali;

    @NotBlank(message = "{validation.person.lastName.required}")
    @Size(max =  100, message = "{validation.person.lastName.size}")
    private String lastName;
    @Size(max = 100, message = "{validation.person.lastName.size}")
    private String lastNameNepali;

    @Column(length = 100)
    private String nickname;

    private String gender;
    private LocalDate birthDate;
    private LocalDate deathDate;

    @Column(length = 255)
    private String photoPath;

    @Column(length = 255)
    private String birthPlace;

    @Column(length = 500)
    private String currentAddress;

    @Column(length = 100)
    private String gotra;

    @Column(length = 4000)
    private String notes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
    public String getGender() {
        return gender;
    }
    public void setGender(String gender) {
        this.gender = gender;
    }
    public String getNickname() {
        return nickname;
    }
    public void setNickname(String nickname) {
        this.nickname = nickname;
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
    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
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
    public String getGotra() {
        return gotra;
    }
    public void setGotra(String gotra) {
        this.gotra = gotra;
    }

    public Integer getGenerationNumber() {
        return generationNumber;
    }
    public void setGenerationNumber(Integer generationNumber) {
        this.generationNumber = generationNumber;
    }
}
