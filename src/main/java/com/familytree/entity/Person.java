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

    private String gender;
    private LocalDate birthDate;
    private LocalDate deathDate;

    @Column(length = 1000)
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

    public Integer getGenerationNumber() {
        return generationNumber;
    }
    public void setGenerationNumber(Integer generationNumber) {
        this.generationNumber = generationNumber;
    }
}
