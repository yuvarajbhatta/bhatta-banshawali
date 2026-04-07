package com.familytree.controller;

import com.familytree.dto.SignupForm;
import com.familytree.services.AppUserService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final AppUserService appUserService;

    public AuthController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (isAuthenticated(authentication)) {
            return "redirect:/";
        }
        return "login";
    }

    @GetMapping("/signup")
    public String signup(Model model, Authentication authentication) {
        if (isAuthenticated(authentication)) {
            return "redirect:/";
        }
        if (!model.containsAttribute("signupForm")) {
            model.addAttribute("signupForm", new SignupForm());
        }
        return "signup";
    }

    @PostMapping("/signup")
    public String registerUser(@Valid @ModelAttribute("signupForm") SignupForm signupForm,
                               BindingResult bindingResult) {
        if (!signupForm.passwordsMatch()) {
            bindingResult.rejectValue("confirmPassword", "signup.confirmPassword.mismatch",
                    "Passwords do not match.");
        }

        if (bindingResult.hasErrors()) {
            return "signup";
        }

        try {
            appUserService.registerUser(signupForm.getUsername(), signupForm.getPassword());
        } catch (IllegalArgumentException exception) {
            bindingResult.rejectValue("username", "signup.username.duplicate", exception.getMessage());
            return "signup";
        }

        return "redirect:/login?registered";
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
