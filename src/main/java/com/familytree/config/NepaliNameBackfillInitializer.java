package com.familytree.config;

import com.familytree.services.PersonService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NepaliNameBackfillInitializer {

    @Bean
    public CommandLineRunner nepaliNameBackfillRunner(PersonService personService, AppProperties appProperties) {
        return args -> {
            if (!appProperties.getNames().isBackfillMissingNepaliOnStartup()) {
                return;
            }

            personService.backfillMissingNepaliNames();
        };
    }
}
