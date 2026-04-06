package com.familytree.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        //model.addAttribute("title", "Family Tree");
        model.addAttribute("message", "Welcome to Family Tree App");
        return "home";
    }
}
