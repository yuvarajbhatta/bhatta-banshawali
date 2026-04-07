package com.familytree.controller;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;

class HomeControllerTest {

    private final HomeController controller = new HomeController();

    @Test
    void homeAddsWelcomeMessageAndReturnsHomeView() {
        Model model = new ExtendedModelMap();

        String viewName = controller.home(model);

        assertThat(viewName).isEqualTo("home");
    }
}
