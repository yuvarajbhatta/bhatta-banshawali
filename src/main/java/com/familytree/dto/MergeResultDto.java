package com.familytree.dto;

/** Summary of what a successful PersonMergeService#merge actually did. */
public record MergeResultDto(
        Long survivorId,
        int relationshipsRepointed,
        int relationshipsDroppedAsDuplicate,
        int userLinksRepointed,
        int correctionRequestsRepointed
) {
}
