package com.familytree.repository;

import com.familytree.entity.AnnouncementPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementPhotoRepository extends JpaRepository<AnnouncementPhoto, Long> {
    List<AnnouncementPhoto> findByPostIdOrderByUploadedAtDesc(Long postId);

    /** Per-post cap enforcement -- see AnnouncementAdminService. */
    long countByPostId(Long postId);
}
