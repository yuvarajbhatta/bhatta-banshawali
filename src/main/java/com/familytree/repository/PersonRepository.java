package com.familytree.repository;

import com.familytree.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonRepository extends JpaRepository<Person, Long> {
    List<Person> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrFirstNameNepaliContainingIgnoreCaseOrLastNameNepaliContainingIgnoreCase(
            String firstName,
            String lastName,
            String firstNameNepali,
            String lastNameNepali
    );
    List<Person> findAllByOrderByGenerationNumberAscIdAsc();
    List<Person> findAllByOrderByIdAsc();
}
