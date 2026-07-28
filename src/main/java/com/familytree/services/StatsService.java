package com.familytree.services;

import com.familytree.dto.PublicStatsDto;
import com.familytree.repository.PersonRepository;
import org.springframework.stereotype.Service;

@Service
public class StatsService {

    private final PersonRepository personRepository;

    public StatsService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    public PublicStatsDto getPublicStats() {
        long documentedFamilyMembers = personRepository.count();
        long documentedGenerations = personRepository.countDistinctGenerationNumbers();
        Integer oldestDocumentedGeneration = personRepository.findMinGenerationNumber();

        return new PublicStatsDto(documentedFamilyMembers, documentedGenerations, oldestDocumentedGeneration);
    }
}
