package com.familytree.repository;

import com.familytree.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonRepository extends JpaRepository<Person, Long> {
    List<Person> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String firstName, String lastName);
    List<Person> findAllByOrderByGenerationNumberAscIdAsc();
    List<Person> findAllByOrderByIdAsc();
}
