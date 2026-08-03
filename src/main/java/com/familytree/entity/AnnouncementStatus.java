package com.familytree.entity;

/**
 * Simpler than ArticleStatus's four-state workflow (no IN_REVIEW) --
 * announcements are time-sensitive, admin-authored posts, not
 * collaboratively-reviewed pages.
 */
public enum AnnouncementStatus {
    DRAFT,
    PUBLISHED
}
