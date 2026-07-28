package com.familytree.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The unverified username/password self-registration flow that used to live
 * at GET/POST /signup was retired in favor of the verification-based
 * pipeline (SignupController, POST /api/v1/signup, reviewed through
 * /admin/signups) -- see docs/05-auth-and-verification.md. Anyone hitting
 * /signup now lands on the Next.js signup page instead (nginx routes it
 * there; see the banshawali.yrbhatta.com vhost).
 */
@Controller
public class AuthController {

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            // Not "/" -- that path is now routed by nginx to the Next.js
            // public landing page, not this backend (see the
            // banshawali.yrbhatta.com vhost and SecurityConfig's
            // defaultSuccessUrl, which redirects here for the same reason).
            return "redirect:/persons";
        }
        return "login";
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
