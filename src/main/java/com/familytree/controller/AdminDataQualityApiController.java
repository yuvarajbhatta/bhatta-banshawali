package com.familytree.controller;

import com.familytree.dto.DataQualityReportDto;
import com.familytree.services.DataQualityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin data-quality reports (docs/08 Phase 6): missing parents,
 * relationship cycles, unlinked accounts, date issues. Read-only --
 * every fix happens through the existing tools these reports link out to
 * (Relationships, Persons, Accounts). Admin-only via "/api/v1/admin/**".
 */
@RestController
@RequestMapping("/api/v1/admin/data-quality")
public class AdminDataQualityApiController {

    private final DataQualityService dataQualityService;

    public AdminDataQualityApiController(DataQualityService dataQualityService) {
        this.dataQualityService = dataQualityService;
    }

    @GetMapping
    public DataQualityReportDto report() {
        return dataQualityService.buildReport();
    }
}
