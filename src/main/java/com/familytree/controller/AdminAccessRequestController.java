package com.familytree.controller;

import com.familytree.dto.AdminAccessRequestConfirmDto;
import com.familytree.dto.MyAdminAccessRequestStatusDto;
import com.familytree.entity.UserAccount;
import com.familytree.repository.UserAccountRepository;
import com.familytree.services.AdminAccessRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * A member's own view of requesting admin access -- see
 * AdminAccessRequestService. 404s for a legacy AppUser (admin) login,
 * same as MemberProfileController: those accounts already have admin
 * access through a different mechanism entirely and have nothing to
 * request.
 */
@RestController
@RequestMapping("/api/v1/me/admin-access-request")
public class AdminAccessRequestController {

    private final UserAccountRepository userAccountRepository;
    private final AdminAccessRequestService adminAccessRequestService;

    public AdminAccessRequestController(UserAccountRepository userAccountRepository,
                                        AdminAccessRequestService adminAccessRequestService) {
        this.userAccountRepository = userAccountRepository;
        this.adminAccessRequestService = adminAccessRequestService;
    }

    @GetMapping
    public MyAdminAccessRequestStatusDto status(Authentication authentication) {
        return adminAccessRequestService.myStatus(currentAccountId(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void request(Authentication authentication) {
        adminAccessRequestService.request(currentAccountId(authentication));
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(Authentication authentication, @Valid @RequestBody AdminAccessRequestConfirmDto request) {
        // Throws InvalidOrExpiredTokenException (mapped to 400) if the
        // code doesn't match, has expired, or has been guessed wrong too
        // many times.
        adminAccessRequestService.confirmRequest(currentAccountId(authentication), request.code());
    }

    private Long currentAccountId(Authentication authentication) {
        UserAccount account = userAccountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No member profile for this account."));
        return account.getId();
    }
}
