package com.familytree.repository;

import com.familytree.entity.AnnouncementPost;
import com.familytree.entity.AnnouncementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AnnouncementPostRepository extends JpaRepository<AnnouncementPost, Long> {
    List<AnnouncementPost> findAllByOrderByUpdatedAtDesc();

    List<AnnouncementPost> findAllByStatusOrderByPinnedDescPublishedAtDesc(AnnouncementStatus status);

    long countByStatus(AnnouncementStatus status);

    /** Unread-badge count for a member who has visited before -- see AnnouncementService. */
    long countByStatusAndPublishedAtAfter(AnnouncementStatus status, LocalDateTime after);
}
