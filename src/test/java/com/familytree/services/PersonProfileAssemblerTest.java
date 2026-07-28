package com.familytree.services;

import com.familytree.dto.FamilySnapshotDto;
import com.familytree.dto.PersonDetailDto;
import com.familytree.dto.PersonSummaryDto;
import com.familytree.entity.Person;
import com.familytree.entity.Relationship;
import com.familytree.entity.RelationshipType;
import com.familytree.web.PersonDisplayHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonProfileAssemblerTest {

    @Mock
    private RelationshipService relationshipService;

    private final PersonDisplayHelper personDisplay = new PersonDisplayHelper();

    private PersonProfileAssembler assembler() {
        return new PersonProfileAssembler(relationshipService, personDisplay);
    }

    @Test
    void summarizeMapsCoreFields() {
        Person person = new Person();
        person.setId(1L);
        person.setFirstName("Yuva");
        person.setLastName("Bhatta");
        person.setFirstNameNepali("युव");
        person.setLastNameNepali("भट्ट");
        person.setGenerationNumber(8);
        person.setGender("Male");
        person.setBirthDate(LocalDate.of(1995, 6, 15));

        PersonSummaryDto summary = assembler().summarize(person);

        assertThat(summary.id()).isEqualTo(1L);
        assertThat(summary.englishFullName()).isEqualTo("Yuva Bhatta");
        assertThat(summary.nepaliFullName()).isEqualTo("युव भट्ट");
        assertThat(summary.generationNumber()).isEqualTo(8);
        assertThat(summary.gender()).isEqualTo("Male");
        assertThat(summary.birthDate()).isEqualTo(LocalDate.of(1995, 6, 15));
    }

    @Test
    void familySnapshotResolvesFatherMotherSpousesAndChildren() {
        Person person = new Person();
        person.setId(1L);

        Person father = new Person();
        father.setId(2L);
        father.setFirstName("Bhoj");
        Relationship fatherRel = new Relationship();
        fatherRel.setRelatedPerson(father);
        when(relationshipService.getRelationshipsByPersonAndType(person, RelationshipType.FATHER))
                .thenReturn(List.of(fatherRel));
        when(relationshipService.getRelationshipsByPersonAndType(person, RelationshipType.MOTHER))
                .thenReturn(List.of());

        Person spouse = new Person();
        spouse.setId(3L);
        spouse.setFirstName("Sita");
        when(relationshipService.getSpousesForPerson(person)).thenReturn(List.of(spouse));

        Person child = new Person();
        child.setId(4L);
        child.setFirstName("Kiran");
        when(relationshipService.getChildrenForPerson(person)).thenReturn(List.of(child));

        FamilySnapshotDto snapshot = assembler().familySnapshot(person);

        assertThat(snapshot.father().englishFullName()).isEqualTo("Bhoj");
        assertThat(snapshot.mother()).isNull();
        assertThat(snapshot.spouses()).extracting(PersonSummaryDto::englishFullName).containsExactly("Sita");
        assertThat(snapshot.children()).extracting(PersonSummaryDto::englishFullName).containsExactly("Kiran");
    }

    @Test
    void detailIncludesFullFieldsAndFamilySnapshot() {
        Person person = new Person();
        person.setId(5L);
        person.setFirstName("Yuva");
        person.setLastName("Bhatta");
        person.setNickname("YB");
        person.setGender("Male");
        person.setGenerationNumber(8);
        person.setBirthDate(LocalDate.of(1995, 6, 15));
        person.setBirthPlace("Kispang");
        person.setCurrentAddress("Kathmandu");
        person.setNotes("Some notes");
        person.setPhotoPath("/uploads/yuva.jpg");

        when(relationshipService.getRelationshipsByPersonAndType(person, RelationshipType.FATHER)).thenReturn(List.of());
        when(relationshipService.getRelationshipsByPersonAndType(person, RelationshipType.MOTHER)).thenReturn(List.of());
        when(relationshipService.getSpousesForPerson(person)).thenReturn(List.of());
        when(relationshipService.getChildrenForPerson(person)).thenReturn(List.of());

        PersonDetailDto detail = assembler().detail(person);

        assertThat(detail.id()).isEqualTo(5L);
        assertThat(detail.englishFullName()).isEqualTo("Yuva Bhatta");
        assertThat(detail.nickname()).isEqualTo("YB");
        assertThat(detail.birthPlace()).isEqualTo("Kispang");
        assertThat(detail.currentAddress()).isEqualTo("Kathmandu");
        assertThat(detail.notes()).isEqualTo("Some notes");
        assertThat(detail.photoPath()).isEqualTo("/uploads/yuva.jpg");
        assertThat(detail.family()).isNotNull();
        assertThat(detail.family().father()).isNull();
    }
}
