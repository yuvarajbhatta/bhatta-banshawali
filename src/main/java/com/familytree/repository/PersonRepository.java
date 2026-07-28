package com.familytree.repository;

import com.familytree.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PersonRepository extends JpaRepository<Person, Long> {
    @Query("""
            select p from Person p
            where
                lower(coalesce(p.firstName, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.middleName, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.lastName, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.firstNameNepali, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.middleNameNepali, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.lastNameNepali, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.nickname, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(p.notes, '')) like lower(concat('%', :keyword, '%'))
                or (:generationNumber is not null and p.generationNumber = :generationNumber)
            order by p.generationNumber asc, p.id asc
            """)
    List<Person> searchPersons(@Param("keyword") String keyword, @Param("generationNumber") Integer generationNumber);

    List<Person> findAllByOrderByGenerationNumberAscIdAsc();
    List<Person> findAllByOrderByIdAsc();

    @Query("select count(distinct p.generationNumber) from Person p where p.generationNumber is not null")
    long countDistinctGenerationNumbers();

    @Query("select min(p.generationNumber) from Person p where p.generationNumber is not null")
    Integer findMinGenerationNumber();
}
