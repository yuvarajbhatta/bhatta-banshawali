package com.familytree.controller;

import com.familytree.dto.AdminAccessRequestDto;
import com.familytree.services.AdminAccessRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin review queue for AdminAccessRequestService -- an existing
 * admin sees who has asked for admin access and approves or denies it.
 * Admin-only via "/api/v1/admin/**".
 */
@RestController
@RequestMapping("/api/v1/admin/admin-access-requests")
public class AdminAdminAccessRequestApiController {

    private final AdminAccessRequestService adminAccessRequestService;

    public AdminAdminAccessRequestApiController(AdminAccessRequestService adminAccessRequestService) {
        this.adminAccessRequestService = adminAccessRequestService;
    }

    @GetMapping
    public List<AdminAccessRequestDto> list() {
        return adminAccessRequestService.findPending();
    }

    @PostMapping("/{id}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(@PathVariable Long id, Authentication authentication) {
        adminAccessRequestService.approve(id, authentication.getName());
    }

    @PostMapping("/{id}/deny")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deny(@PathVariable Long id, Authentication authentication) {
        adminAccessRequestService.deny(id, authentication.getName());
    }
}
