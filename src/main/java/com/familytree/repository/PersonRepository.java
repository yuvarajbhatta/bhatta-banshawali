package com.familytree.repository;

import com.familytree.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PersonRepository extends JpaRepository<Person, Long> {
    // Per-field checks alone only match a keyword contained entirely
    // within ONE column (first, middle, or last name) -- a two-word
    // search like "Bhoj Raj" against firstName="Bhoj"/middleName="Raj"
    // fails every one of them since neither column holds the full
    // "Bhoj Raj" substring. CONCAT_WS (not plain concat: it skips nulls,
    // so a missing middle name doesn't leave a double space breaking the
    // match) additionally checks the whole name as typically searched.
    //
    // Deliberately a full table scan, not indexed (production-readiness
    // review, LOW): a leading-wildcard LIKE '%keyword%' can't use a plain
    // B-tree index regardless, and a real fix (MySQL FULLTEXT, trigram) would
    // change match semantics -- FULLTEXT doesn't do substring-anywhere
    // matching, so "raj" would stop matching "Bhojraj" the way it does today.
    // Fine at actual family-tree scale (hundreds to low thousands of rows);
    // revisit if that scale assumption ever stops holding.
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
                or lower(function('CONCAT_WS', ' ', p.firstName, p.middleName, p.lastName)) like lower(concat('%', :keyword, '%'))
                or lower(function('CONCAT_WS', ' ', p.firstNameNepali, p.middleNameNepali, p.lastNameNepali)) like lower(concat('%', :keyword, '%'))
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

    /** Either bound may be null (open-ended); both null returns everyone -- see FamilyTreeAssembler. */
    @Query("""
            select p from Person p
            where (:minGeneration is null or p.generationNumber >= :minGeneration)
              and (:maxGeneration is null or p.generationNumber <= :maxGeneration)
            order by p.generationNumber asc, p.id asc
            """)
    List<Person> findByGenerationNumberRange(@Param("minGeneration") Integer minGeneration,
                                              @Param("maxGeneration") Integer maxGeneration);
}
