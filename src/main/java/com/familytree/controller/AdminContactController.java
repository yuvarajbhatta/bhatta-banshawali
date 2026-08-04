package com.familytree.controller;

import com.familytree.dto.AdminContactDto;
import com.familytree.services.AdminContactService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Who to contact for help -- deliberately NOT under "/api/v1/admin/**":
 * every authenticated member needs to read this (see the Help & Contact
 * page's Contact section), not just admins themselves.
 */
@RestController
@RequestMapping("/api/v1/admin-contacts")
public class AdminContactController {

    private final AdminContactService adminContactService;

    public AdminContactController(AdminContactService adminContactService) {
        this.adminContactService = adminContactService;
    }

    @GetMapping
    public List<AdminContactDto> list() {
        return adminContactService.listAdminContacts();
    }
}
