package com.familytree.services;

import com.familytree.dto.PublicStatsDto;
import com.familytree.repository.PersonRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private PersonRepository personRepository;

    @InjectMocks
    private StatsService statsService;

    @Test
    void getPublicStatsAggregatesRepositoryCounts() {
        when(personRepository.count()).thenReturn(42L);
        when(personRepository.countDistinctGenerationNumbers()).thenReturn(7L);
        when(personRepository.findMinGenerationNumber()).thenReturn(1);

        PublicStatsDto stats = statsService.getPublicStats();

        assertThat(stats.documentedFamilyMembers()).isEqualTo(42L);
        assertThat(stats.documentedGenerations()).isEqualTo(7L);
        assertThat(stats.oldestDocumentedGeneration()).isEqualTo(1);
    }

    @Test
    void getPublicStatsHandlesNoGenerationDataYet() {
        when(personRepository.count()).thenReturn(0L);
        when(personRepository.countDistinctGenerationNumbers()).thenReturn(0L);
        when(personRepository.findMinGenerationNumber()).thenReturn(null);

        PublicStatsDto stats = statsService.getPublicStats();

        assertThat(stats.oldestDocumentedGeneration()).isNull();
    }
}
