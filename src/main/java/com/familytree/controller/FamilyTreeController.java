package com.familytree.controller;

import com.familytree.dto.FamilyTreeDto;
import com.familytree.services.FamilyTreeAssembler;
import com.familytree.services.ViewerContext;
import com.familytree.services.ViewerContextResolver;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Whole-family graph for the interactive tree view (docs/08 Phase 5).
 * Authenticated-only via the default anyRequest().authenticated() rule
 * in SecurityConfig, same as PersonApiController -- no new security
 * matcher needed. Per-viewer field redaction is handled by
 * FamilyTreeAssembler, not here.
 */
@RestController
@RequestMapping("/api/v1/family-tree")
public class FamilyTreeController {

    private final FamilyTreeAssembler familyTreeAssembler;
    private final ViewerContextResolver viewerContextResolver;

    public FamilyTreeController(FamilyTreeAssembler familyTreeAssembler, ViewerContextResolver viewerContextResolver) {
        this.familyTreeAssembler = familyTreeAssembler;
        this.viewerContextResolver = viewerContextResolver;
    }

    @GetMapping
    public FamilyTreeDto tree(Authentication authentication,
                              @RequestParam(required = false) Integer minGeneration,
                              @RequestParam(required = false) Integer maxGeneration) {
        if (minGeneration != null && maxGeneration != null && minGeneration > maxGeneration) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minGeneration must not be greater than maxGeneration.");
        }
        ViewerContext viewer = viewerContextResolver.resolve(authentication);
        return familyTreeAssembler.buildTree(viewer, minGeneration, maxGeneration);
    }
}
