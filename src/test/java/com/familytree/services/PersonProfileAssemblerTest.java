package com.familytree.services;

import com.familytree.dto.FamilySnapshotDto;
import com.familytree.dto.PersonDetailDto;
import com.familytree.dto.PersonSummaryDto;
import com.familytree.entity.Person;
import com.familytree.web.PersonDisplayHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonProfileAssemblerTest {

    private static final ViewerContext ADMIN = new ViewerContext(true, null);
    private static final ViewerContext STRANGER = new ViewerContext(false, 9999L);
    private static final ViewerContext NO_LINK = new ViewerContext(false, null);

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

        PersonSummaryDto summary = assembler().summarize(person, ADMIN);

        assertThat(summary.id()).isEqualTo(1L);
        assertThat(summary.englishFullName()).isEqualTo("Yuva Bhatta");
        assertThat(summary.nepaliFullName()).isEqualTo("युव भट्ट");
        assertThat(summary.generationNumber()).isEqualTo(8);
        assertThat(summary.gender()).isEqualTo("Male");
        assertThat(summary.birthDate()).isEqualTo(LocalDate.of(1995, 6, 15));
    }

    @Test
    void summarizeHidesBirthDateFromAStrangerViewer() {
        Person person = new Person();
        person.setId(1L);
        person.setFirstName("Yuva");
        person.setLastName("Bhatta");
        person.setBirthDate(LocalDate.of(1995, 6, 15));

        PersonSummaryDto summary = assembler().summarize(person, STRANGER);

        assertThat(summary.birthDate()).isNull();
        assertThat(summary.englishFullName()).isEqualTo("Yuva Bhatta");
    }

    @Test
    void summarizeShowsBirthDateWhenViewingOwnLinkedPerson() {
        Person person = new Person();
        person.setId(42L);
        person.setBirthDate(LocalDate.of(1995, 6, 15));

        PersonSummaryDto summary = assembler().summarize(person, new ViewerContext(false, 42L));

        assertThat(summary.birthDate()).isEqualTo(LocalDate.of(1995, 6, 15));
    }

    @Test
    void summarizeHidesBirthDateFromAnUnlinkedViewer() {
        Person person = new Person();
        person.setId(1L);
        person.setBirthDate(LocalDate.of(1995, 6, 15));

        PersonSummaryDto summary = assembler().summarize(person, NO_LINK);

        assertThat(summary.birthDate()).isNull();
    }

    @Test
    void summarizeForSearchIncludesFatherNameAsParentHint() {
        Person person = new Person();
        person.setId(1L);
        person.setFirstName("Yuva");
        person.setLastName("Bhatta");

        Person father = new Person();
        father.setId(2L);
        father.setFirstName("Bhoj");
        father.setLastName("Bhatta");
        when(relationshipService.getFatherForPerson(person)).thenReturn(Optional.of(father));

        PersonSummaryDto summary = assembler().summarizeForSearch(person, ADMIN);

        assertThat(summary.parentHint()).isEqualTo("Bhoj Bhatta");
    }

    @Test
    void summarizeForSearchLeavesParentHintNullWhenNoFatherRecorded() {
        Person person = new Person();
        person.setId(1L);
        person.setFirstName("Yuva");

        when(relationshipService.getFatherForPerson(person)).thenReturn(Optional.empty());

        PersonSummaryDto summary = assembler().summarizeForSearch(person, ADMIN);

        assertThat(summary.parentHint()).isNull();
    }

    @Test
    void summarizeNeverPopulatesParentHint() {
        Person person = new Person();
        person.setId(1L);
        person.setFirstName("Yuva");

        PersonSummaryDto summary = assembler().summarize(person, ADMIN);

        assertThat(summary.parentHint()).isNull();
    }

    @Test
    void familySnapshotResolvesFatherMotherSpousesAndChildren() {
        Person person = new Person();
        person.setId(1L);

        Person father = new Person();
        father.setId(2L);
        father.setFirstName("Bhoj");
        when(relationshipService.getFatherForPerson(person)).thenReturn(Optional.of(father));
        when(relationshipService.getMotherForPerson(person)).thenReturn(Optional.empty());

        Person spouse = new Person();
        spouse.setId(3L);
        spouse.setFirstName("Sita");
        when(relationshipService.getSpousesForPerson(person)).thenReturn(List.of(spouse));

        Person child = new Person();
        child.setId(4L);
        child.setFirstName("Kiran");
        when(relationshipService.getChildrenForPerson(person)).thenReturn(List.of(child));

        FamilySnapshotDto snapshot = assembler().familySnapshot(person, ADMIN);

        assertThat(snapshot.father().englishFullName()).isEqualTo("Bhoj");
        assertThat(snapshot.mother()).isNull();
        assertThat(snapshot.spouses()).extracting(PersonSummaryDto::englishFullName).containsExactly("Sita");
        assertThat(snapshot.children()).extracting(PersonSummaryDto::englishFullName).containsExactly("Kiran");
    }

    @Test
    void detailIncludesFullFieldsAndFamilySnapshotForAdmin() {
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

        when(relationshipService.getFatherForPerson(person)).thenReturn(Optional.empty());
        when(relationshipService.getMotherForPerson(person)).thenReturn(Optional.empty());
        when(relationshipService.getSpousesForPerson(person)).thenReturn(List.of());
        when(relationshipService.getChildrenForPerson(person)).thenReturn(List.of());

        PersonDetailDto detail = assembler().detail(person, ADMIN);

        assertThat(detail.id()).isEqualTo(5L);
        assertThat(detail.englishFullName()).isEqualTo("Yuva Bhatta");
        assertThat(detail.nickname()).isEqualTo("YB");
        assertThat(detail.birthDate()).isEqualTo(LocalDate.of(1995, 6, 15));
        assertThat(detail.birthPlace()).isEqualTo("Kispang");
        assertThat(detail.currentAddress()).isEqualTo("Kathmandu");
        assertThat(detail.notes()).isEqualTo("Some notes");
        assertThat(detail.photoPath()).isEqualTo("/uploads/yuva.jpg");
        assertThat(detail.family()).isNotNull();
        assertThat(detail.family().father()).isNull();
    }

    @Test
    void detailHidesBirthDateAndCurrentAddressFromAStrangerViewer() {
        Person person = new Person();
        person.setId(5L);
        person.setFirstName("Yuva");
        person.setLastName("Bhatta");
        person.setBirthDate(LocalDate.of(1995, 6, 15));
        person.setBirthPlace("Kispang");
        person.setCurrentAddress("Kathmandu");
        person.setDeathDate(null);

        when(relationshipService.getFatherForPerson(person)).thenReturn(Optional.empty());
        when(relationshipService.getMotherForPerson(person)).thenReturn(Optional.empty());
        when(relationshipService.getSpousesForPerson(person)).thenReturn(List.of());
        when(relationshipService.getChildrenForPerson(person)).thenReturn(List.of());

        PersonDetailDto detail = assembler().detail(person, STRANGER);

        assertThat(detail.birthDate()).isNull();
        assertThat(detail.currentAddress()).isNull();
        // Not sensitive by this rule -- birthPlace (general area, not exact
        // contact info) stays visible so the directory remains genuinely useful.
        assertThat(detail.birthPlace()).isEqualTo("Kispang");
    }

    @Test
    void detailShowsSensitiveFieldsWhenViewingOwnLinkedPerson() {
        Person person = new Person();
        person.setId(77L);
        person.setBirthDate(LocalDate.of(1995, 6, 15));
        person.setCurrentAddress("Kathmandu");

        when(relationshipService.getFatherForPerson(person)).thenReturn(Optional.empty());
        when(relationshipService.getMotherForPerson(person)).thenReturn(Optional.empty());
        when(relationshipService.getSpousesForPerson(person)).thenReturn(List.of());
        when(relationshipService.getChildrenForPerson(person)).thenReturn(List.of());

        PersonDetailDto detail = assembler().detail(person, new ViewerContext(false, 77L));

        assertThat(detail.birthDate()).isEqualTo(LocalDate.of(1995, 6, 15));
        assertThat(detail.currentAddress()).isEqualTo("Kathmandu");
    }

    @Test
    void familySnapshotRedactsBirthDatesOfRelativesFromAStrangerViewer() {
        Person person = new Person();
        person.setId(1L);

        Person father = new Person();
        father.setId(2L);
        father.setFirstName("Bhoj");
        father.setBirthDate(LocalDate.of(1965, 1, 1));
        when(relationshipService.getFatherForPerson(person)).thenReturn(Optional.of(father));
        when(relationshipService.getMotherForPerson(person)).thenReturn(Optional.empty());
        when(relationshipService.getSpousesForPerson(person)).thenReturn(List.of());
        when(relationshipService.getChildrenForPerson(person)).thenReturn(List.of());

        FamilySnapshotDto snapshot = assembler().familySnapshot(person, STRANGER);

        assertThat(snapshot.father().englishFullName()).isEqualTo("Bhoj");
        assertThat(snapshot.father().birthDate()).isNull();
    }

    @Test
    void ancestorChainWalksTheFatherLineIncludingThePersonItself() {
        // A real, multi-generation chain -- proves the walk doesn't stop
        // after one or two hops, which is the whole point of this method.
        Person yuva = new Person();
        yuva.setId(1L);
        yuva.setFirstName("Yuva Raj");
        yuva.setLastName("Bhatta");

        Person father = new Person();
        father.setId(2L);
        father.setFirstName("Bhoj Raj");
        father.setLastName("Bhatta");

        Person grandfather = new Person();
        grandfather.setId(3L);
        grandfather.setFirstName("Jhanka Nath");
        grandfather.setLastName("Bhatta");

        Person greatGrandfather = new Person();
        greatGrandfather.setId(4L);
        greatGrandfather.setFirstName("Megh Nath");
        greatGrandfather.setLastName("Bhatta");

        when(relationshipService.getFatherForPerson(yuva)).thenReturn(Optional.of(father));
        when(relationshipService.getFatherForPerson(father)).thenReturn(Optional.of(grandfather));
        when(relationshipService.getFatherForPerson(grandfather)).thenReturn(Optional.of(greatGrandfather));
        when(relationshipService.getFatherForPerson(greatGrandfather)).thenReturn(Optional.empty());

        List<PersonSummaryDto> chain = assembler().ancestorChain(yuva, ADMIN);

        assertThat(chain).extracting(PersonSummaryDto::englishFullName)
                .containsExactly("Yuva Raj Bhatta", "Bhoj Raj Bhatta", "Jhanka Nath Bhatta", "Megh Nath Bhatta");
    }

    @Test
    void ancestorChainIsJustThePersonWhenNoFatherIsRecorded() {
        Person person = new Person();
        person.setId(1L);
        person.setFirstName("Latadev");
        person.setLastName("Bhatta");
        when(relationshipService.getFatherForPerson(person)).thenReturn(Optional.empty());

        List<PersonSummaryDto> chain = assembler().ancestorChain(person, ADMIN);

        assertThat(chain).extracting(PersonSummaryDto::englishFullName).containsExactly("Latadev Bhatta");
    }

    @Test
    void ancestorChainStopsAtASafetyDepthRatherThanLoopingForeverOnACycle() {
        // Historical data could in principle contain a cycle predating
        // saveRelationshipWithAutoLinks's cycle guard -- this must
        // terminate rather than hang.
        Person a = new Person();
        a.setId(1L);
        a.setFirstName("A");
        Person b = new Person();
        b.setId(2L);
        b.setFirstName("B");

        when(relationshipService.getFatherForPerson(a)).thenReturn(Optional.of(b));
        when(relationshipService.getFatherForPerson(b)).thenReturn(Optional.of(a));

        List<PersonSummaryDto> chain = assembler().ancestorChain(a, ADMIN);

        assertThat(chain).hasSize(50);
    }
}
