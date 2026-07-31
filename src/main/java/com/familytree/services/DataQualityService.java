package com.familytree.services;

import com.familytree.dto.AdminUserAccountDto;
import com.familytree.dto.DataQualityReportDto;
import com.familytree.dto.DateIssueDto;
import com.familytree.dto.ParentGapDto;
import com.familytree.dto.RelationshipCycleDto;
import com.familytree.entity.Person;
import com.familytree.entity.Relationship;
import com.familytree.entity.RelationshipType;
import com.familytree.repository.PersonRepository;
import com.familytree.repository.RelationshipRepository;
import com.familytree.web.PersonDisplayHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Admin data-quality reports (docs/08 Phase 6): missing parents, relationship
 * cycles, unlinked user accounts, and date issues. Read-only -- flags for
 * review, never auto-corrects (see docs/04-data-model.md's integrity-rules
 * note that date inconsistencies are "flagged for review, not blocked").
 *
 * Deliberately not wired into AdminSummaryController's nav badge counts:
 * the cycle scan and parent-gap scan each read the full person/relationship
 * tables, and AdminSummaryDto is computed on every admin page load for the
 * sidebar -- doing that work on every page view instead of only when this
 * report is opened would be a real perf trap at any non-trivial data size.
 */
@Service
public class DataQualityService {

    public static final String ISSUE_MISSING_BIRTH_DATE = "MISSING_BIRTH_DATE";
    public static final String ISSUE_DEATH_BEFORE_BIRTH = "DEATH_BEFORE_BIRTH";
    public static final String ISSUE_FUTURE_BIRTH_DATE = "FUTURE_BIRTH_DATE";
    public static final String ISSUE_IMPLAUSIBLE_PARENT_AGE_GAP = "IMPLAUSIBLE_PARENT_AGE_GAP";

    private static final int MIN_PLAUSIBLE_PARENT_AGE_AT_BIRTH = 12;
    private static final int MAX_PLAUSIBLE_PARENT_AGE_AT_BIRTH = 75;

    private final PersonRepository personRepository;
    private final RelationshipRepository relationshipRepository;
    private final RelationshipService relationshipService;
    private final UserAccountAdminService userAccountAdminService;
    private final PersonDisplayHelper personDisplay;

    public DataQualityService(PersonRepository personRepository,
                              RelationshipRepository relationshipRepository,
                              RelationshipService relationshipService,
                              UserAccountAdminService userAccountAdminService,
                              PersonDisplayHelper personDisplay) {
        this.personRepository = personRepository;
        this.relationshipRepository = relationshipRepository;
        this.relationshipService = relationshipService;
        this.userAccountAdminService = userAccountAdminService;
        this.personDisplay = personDisplay;
    }

    public DataQualityReportDto buildReport() {
        List<Person> people = personRepository.findAll();
        return new DataQualityReportDto(
                findParentGaps(people),
                findCycles(people),
                findUnlinkedAccounts(),
                findDateIssues(people)
        );
    }

    /**
     * People with fewer than 2 recorded parents. Does not try to exclude
     * legitimate roots (generationNumber is included so the admin can do
     * that judgment call themselves) -- see this class's Javadoc and
     * docs/07-migration-plan.md on why that heuristic is left to a human.
     */
    private List<ParentGapDto> findParentGaps(List<Person> people) {
        List<ParentGapDto> gaps = new ArrayList<>();
        for (Person person : people) {
            int knownParents = relationshipService.getParentsForPerson(person).size();
            if (knownParents < 2) {
                gaps.add(new ParentGapDto(person.getId(), personDisplay.englishFullName(person),
                        person.getGenerationNumber(), knownParents));
            }
        }
        return gaps;
    }

    private List<AdminUserAccountDto> findUnlinkedAccounts() {
        return userAccountAdminService.listAll().stream()
                .filter(account -> account.linkedPersonId() == null)
                .toList();
    }

    private List<DateIssueDto> findDateIssues(List<Person> people) {
        List<DateIssueDto> issues = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Person person : people) {
            String name = personDisplay.englishFullName(person);
            LocalDate birth = person.getBirthDate();
            LocalDate death = person.getDeathDate();

            if (birth == null) {
                issues.add(new DateIssueDto(person.getId(), name, ISSUE_MISSING_BIRTH_DATE,
                        "No birth date recorded."));
            } else if (birth.isAfter(today)) {
                issues.add(new DateIssueDto(person.getId(), name, ISSUE_FUTURE_BIRTH_DATE,
                        "Birth date " + birth + " is in the future."));
            }

            if (birth != null && death != null && death.isBefore(birth)) {
                issues.add(new DateIssueDto(person.getId(), name, ISSUE_DEATH_BEFORE_BIRTH,
                        "Death date " + death + " is before birth date " + birth + "."));
            }

            if (birth != null) {
                for (Person parent : relationshipService.getParentsForPerson(person)) {
                    LocalDate parentBirth = parent.getBirthDate();
                    if (parentBirth == null) {
                        continue;
                    }
                    int ageAtBirth = Period.between(parentBirth, birth).getYears();
                    if (ageAtBirth < MIN_PLAUSIBLE_PARENT_AGE_AT_BIRTH || ageAtBirth > MAX_PLAUSIBLE_PARENT_AGE_AT_BIRTH) {
                        issues.add(new DateIssueDto(person.getId(), name, ISSUE_IMPLAUSIBLE_PARENT_AGE_GAP,
                                personDisplay.englishFullName(parent) + " would have been " + ageAtBirth
                                        + " years old at " + name + "'s birth."));
                    }
                }
            }
        }
        return issues;
    }

    /**
     * Batch graph traversal (three-color DFS), not N independent
     * RelationshipService#isAncestor calls -- O(V+E) instead of O(V*(V+E)).
     * Returns one witness cycle per back-edge found, deduped by node-set so
     * a single strongly-connected tangle with multiple internal back-edges
     * isn't reported as several distinct "cycles".
     */
    private List<RelationshipCycleDto> findCycles(List<Person> people) {
        Map<Long, Person> personMap = new LinkedHashMap<>();
        for (Person person : people) {
            personMap.put(person.getId(), person);
        }

        Map<Long, List<Long>> parentToChildren = buildParentToChildrenGraph();

        Map<Long, Integer> color = new HashMap<>();
        Deque<Long> path = new ArrayDeque<>();
        List<List<Long>> rawCycles = new ArrayList<>();

        for (Person person : people) {
            if (!color.containsKey(person.getId())) {
                dfsDetectCycle(person.getId(), parentToChildren, color, path, rawCycles);
            }
        }

        Map<Set<Long>, List<Long>> distinctCycles = new LinkedHashMap<>();
        for (List<Long> cycle : rawCycles) {
            distinctCycles.putIfAbsent(new HashSet<>(cycle), cycle);
        }

        List<RelationshipCycleDto> result = new ArrayList<>();
        for (List<Long> cycle : distinctCycles.values()) {
            List<String> names = cycle.stream()
                    .map(id -> personDisplay.englishFullName(personMap.get(id)))
                    .toList();
            result.add(new RelationshipCycleDto(cycle, names));
        }
        return result;
    }

    /**
     * CHILD-type rows (person=parent, relatedPerson=child) are the reliable
     * direction -- saveRelationshipWithAutoLinks always creates them
     * alongside FATHER/MOTHER. FATHER/MOTHER rows only add an edge here if
     * no reciprocal CHILD row was already captured, for legacy data that
     * predates auto-linking -- same reasoning RelationshipService#getParentsForPerson
     * already uses.
     */
    private Map<Long, List<Long>> buildParentToChildrenGraph() {
        Map<Long, List<Long>> parentToChildren = new LinkedHashMap<>();
        Set<String> seenPairs = new HashSet<>();
        List<Relationship> all = relationshipRepository.findAll();

        for (Relationship relationship : all) {
            if (relationship.getRelationshipType() == RelationshipType.CHILD) {
                Long parentId = relationship.getPerson().getId();
                Long childId = relationship.getRelatedPerson().getId();
                seenPairs.add(parentId + ":" + childId);
                addEdge(parentToChildren, parentId, childId);
            }
        }
        for (Relationship relationship : all) {
            if (relationship.getRelationshipType() == RelationshipType.FATHER
                    || relationship.getRelationshipType() == RelationshipType.MOTHER) {
                Long childId = relationship.getPerson().getId();
                Long parentId = relationship.getRelatedPerson().getId();
                if (seenPairs.add(parentId + ":" + childId)) {
                    addEdge(parentToChildren, parentId, childId);
                }
            }
        }
        return parentToChildren;
    }

    private void addEdge(Map<Long, List<Long>> graph, Long parentId, Long childId) {
        graph.computeIfAbsent(parentId, key -> new ArrayList<>()).add(childId);
    }

    private void dfsDetectCycle(Long personId, Map<Long, List<Long>> graph, Map<Long, Integer> color,
                               Deque<Long> path, List<List<Long>> cycles) {
        color.put(personId, 1); // GRAY: on the current path
        path.addLast(personId);

        for (Long childId : graph.getOrDefault(personId, List.of())) {
            Integer childColor = color.get(childId);
            if (childColor == null) {
                dfsDetectCycle(childId, graph, color, path, cycles);
            } else if (childColor == 1) {
                List<Long> pathSnapshot = new ArrayList<>(path);
                int backEdgeIndex = pathSnapshot.indexOf(childId);
                cycles.add(new ArrayList<>(pathSnapshot.subList(backEdgeIndex, pathSnapshot.size())));
            }
        }

        color.put(personId, 2); // BLACK: fully explored
        path.removeLast();
    }
}
