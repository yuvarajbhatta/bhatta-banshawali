package com.familytree.dto;

import java.util.List;

/**
 * One candidate on the signup review screen -- either an existing Person
 * to link the account to, or an existing father to create a brand-new
 * Person under. ancestorChain walks the FATHER line as far back as it's
 * recorded, starting with person itself, so the admin can visually
 * recognize the correct lineage when several candidates share the same
 * name (see PersonProfileAssembler.ancestorChain).
 */
public record MatchCandidateDto(
        PersonSummaryDto person,
        List<PersonSummaryDto> ancestorChain
) {
}
