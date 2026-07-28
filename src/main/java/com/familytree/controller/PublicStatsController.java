package com.familytree.controller;

import com.familytree.dto.PublicStatsDto;
import com.familytree.services.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, unauthenticated aggregate statistics for the landing page. Never
 * exposes names or individual records -- see StatsService.
 */
@RestController
@RequestMapping("/api/v1/public-stats")
public class PublicStatsController {

    private final StatsService statsService;

    public PublicStatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public PublicStatsDto getPublicStats() {
        return statsService.getPublicStats();
    }
}
