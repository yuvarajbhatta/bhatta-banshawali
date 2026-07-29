package com.familytree.controller;

import com.familytree.dto.AdminAccountSignupInfoUpdateDto;
import com.familytree.dto.AdminUserAccountDto;
import com.familytree.dto.LinkAccountRequestDto;
import com.familytree.services.UserAccountAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * "Manage User Accounts" (docs/08 Phase 6): everything an admin can do
 * to a UserAccount from one place -- link/unlink to a Person, correct
 * the applicant's submitted signup info, revoke (disable) or restore
 * (enable) login access, or permanently delete the account. Admin-only
 * via "/api/v1/admin/**".
 */
@RestController
@RequestMapping("/api/v1/admin/accounts")
public class AdminUserAccountApiController {

    private final UserAccountAdminService userAccountAdminService;

    public AdminUserAccountApiController(UserAccountAdminService userAccountAdminService) {
        this.userAccountAdminService = userAccountAdminService;
    }

    @GetMapping
    public List<AdminUserAccountDto> list() {
        return userAccountAdminService.listAll();
    }

    @PostMapping("/{id}/link")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void link(@PathVariable Long id, @Valid @RequestBody LinkAccountRequestDto request, Authentication authentication) {
        userAccountAdminService.link(id, request.getPersonId(), authentication.getName());
    }

    @PostMapping("/{id}/unlink")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(@PathVariable Long id, Authentication authentication) {
        userAccountAdminService.unlink(id, authentication.getName());
    }

    @PutMapping("/{id}/signup-info")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateSignupInfo(@PathVariable Long id, @Valid @RequestBody AdminAccountSignupInfoUpdateDto request,
                                 Authentication authentication) {
        userAccountAdminService.updateSignupInfo(id, request, authentication.getName());
    }

    @PostMapping("/{id}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable Long id, Authentication authentication) {
        userAccountAdminService.disable(id, authentication.getName());
    }

    @PostMapping("/{id}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enable(@PathVariable Long id, Authentication authentication) {
        userAccountAdminService.enable(id, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        userAccountAdminService.delete(id, authentication.getName());
    }
}
