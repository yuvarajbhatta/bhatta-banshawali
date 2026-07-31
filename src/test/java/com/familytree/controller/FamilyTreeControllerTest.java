package com.familytree.controller;

import com.familytree.dto.FamilyTreeDto;
import com.familytree.repository.UserAccountRepository;
import com.familytree.repository.UserPersonLinkRepository;
import com.familytree.services.FamilyTreeAssembler;
import com.familytree.services.ViewerContextResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyTreeControllerTest {

    @Mock
    private FamilyTreeAssembler familyTreeAssembler;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserPersonLinkRepository userPersonLinkRepository;

    @Mock
    private Authentication authentication;

    private FamilyTreeController controller() {
        return new FamilyTreeController(familyTreeAssembler,
                new ViewerContextResolver(userAccountRepository, userPersonLinkRepository));
    }

    @Test
    void returnsTreeBuiltForResolvedViewer() {
        doReturnAdmin();
        FamilyTreeDto expected = new FamilyTreeDto(List.of(), null);
        when(familyTreeAssembler.buildTree(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull())).thenReturn(expected);

        FamilyTreeDto result = controller().tree(authentication, null, null);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void resolvesViewerAsAdminWhenAuthorityPresent() {
        doReturnAdmin();
        when(familyTreeAssembler.buildTree(org.mockito.ArgumentMatchers.argThat(viewer -> viewer.isAdmin()),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(new FamilyTreeDto(List.of(), null));

        controller().tree(authentication, null, null);
    }

    @Test
    void passesGenerationWindowParamsThroughToAssembler() {
        doReturnAdmin();
        when(familyTreeAssembler.buildTree(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(2),
                org.mockito.ArgumentMatchers.eq(5))).thenReturn(new FamilyTreeDto(List.of(), null));

        controller().tree(authentication, 2, 5);

        org.mockito.Mockito.verify(familyTreeAssembler).buildTree(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(2),
                org.mockito.ArgumentMatchers.eq(5));
    }

    @Test
    void rejectsMinGenerationGreaterThanMaxGeneration() {
        assertThatThrownBy(() -> controller().tree(authentication, 5, 2))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    private void doReturnAdmin() {
        org.mockito.Mockito.doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(authentication).getAuthorities();
        when(authentication.getName()).thenReturn("admin@example.com");
        when(userAccountRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
    }
}
