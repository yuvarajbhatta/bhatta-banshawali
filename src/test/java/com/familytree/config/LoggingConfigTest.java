package com.familytree.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingConfigTest {

    private final LoggingConfig loggingConfig = new LoggingConfig();

    @Test
    void registersCorrelationIdFilterAheadOfEverythingElse() {
        FilterRegistrationBean<CorrelationIdFilter> registration = loggingConfig.correlationIdFilterRegistration();

        assertThat(registration.getFilter()).isInstanceOf(CorrelationIdFilter.class);
        assertThat(registration.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
        assertThat(registration.getUrlPatterns()).contains("/*");
    }
}
