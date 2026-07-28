package com.familytree.web;

import com.familytree.entity.Person;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component("personDisplay")
public class PersonDisplayHelper {

    public String fullName(Person person, Locale locale) {
        if (person == null) {
            return "";
        }

        boolean nepali = locale != null && "ne".equalsIgnoreCase(locale.getLanguage());
        if (nepali) {
            String nepaliName = nepaliFullName(person);
            if (!nepaliName.isBlank()) {
                return nepaliName;
            }
        }
        return englishFullName(person);
    }

    public String englishFullName(Person person) {
        if (person == null) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        appendIfPresent(parts, person.getFirstName());
        appendIfPresent(parts, person.getMiddleName());
        appendIfPresent(parts, person.getLastName());
        return String.join(" ", parts);
    }

    public String nepaliFullName(Person person) {
        if (person == null) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        appendIfPresent(parts, person.getFirstNameNepali());
        appendIfPresent(parts, person.getMiddleNameNepali());
        appendIfPresent(parts, person.getLastNameNepali());
        return String.join(" ", parts);
    }

    public boolean hasNepaliName(Person person) {
        return person != null && !nepaliFullName(person).isBlank();
    }

    public String givenName(Person person, Locale locale) {
        if (person == null) {
            return "";
        }

        boolean nepali = locale != null && "ne".equalsIgnoreCase(locale.getLanguage());
        if (nepali && person.getFirstNameNepali() != null && !person.getFirstNameNepali().isBlank()) {
            return person.getFirstNameNepali();
        }
        return person.getFirstName() == null ? "" : person.getFirstName();
    }

    public String familyName(Person person, Locale locale) {
        if (person == null) {
            return "";
        }

        boolean nepali = locale != null && "ne".equalsIgnoreCase(locale.getLanguage());
        if (nepali && person.getLastNameNepali() != null && !person.getLastNameNepali().isBlank()) {
            return person.getLastNameNepali();
        }
        return person.getLastName() == null ? "" : person.getLastName();
    }

    private void appendIfPresent(List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value.trim());
        }
    }
}
