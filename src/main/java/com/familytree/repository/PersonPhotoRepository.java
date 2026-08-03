package com.familytree.repository;

import com.familytree.entity.PersonPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PersonPhotoRepository extends JpaRepository<PersonPhoto, Long> {
    List<PersonPhoto> findByPersonIdOrderByUploadedAtDesc(Long personId);

    /** Per-person cap enforcement -- see PersonPhotoService. */
    long countByPersonId(Long personId);
}
