package com.familytree.config;

import com.familytree.services.PersonService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NepaliNameBackfillInitializerTest {

    private final NepaliNameBackfillInitializer initializer = new NepaliNameBackfillInitializer();

    @Test
    void backfillsMissingNepaliNamesWhenEnabled() throws Exception {
        PersonService personService = mock(PersonService.class);
        when(personService.backfillMissingNepaliNames()).thenReturn(3);
        AppProperties appProperties = new AppProperties();
        appProperties.getNames().setBackfillMissingNepaliOnStartup(true);

        initializer.nepaliNameBackfillRunner(personService, appProperties).run();

        verify(personService).backfillMissingNepaliNames();
    }

    @Test
    void skipsBackfillWhenDisabled() throws Exception {
        PersonService personService = mock(PersonService.class);
        AppProperties appProperties = new AppProperties();
        appProperties.getNames().setBackfillMissingNepaliOnStartup(false);

        initializer.nepaliNameBackfillRunner(personService, appProperties).run();

        verifyNoInteractions(personService);
    }
}
