package com.familytree.repository;

import com.familytree.entity.Permission;
import com.familytree.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:role-repo;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ImportAutoConfiguration(exclude = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@Transactional
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Test
    void findByNameReturnsSavedRoleWithPermissions() {
        Permission permission = new Permission();
        permission.setName("VIEW_DASHBOARD");
        permission = permissionRepository.save(permission);

        Role role = new Role();
        role.setName("VERIFIED_MEMBER");
        role.setPermissions(Set.of(permission));
        roleRepository.save(role);

        assertThat(roleRepository.findByName("VERIFIED_MEMBER"))
                .hasValueSatisfying(saved -> assertThat(saved.getPermissions())
                        .extracting(Permission::getName)
                        .containsExactly("VIEW_DASHBOARD"));
    }

    @Test
    void findByNameReturnsEmptyWhenMissing() {
        assertThat(roleRepository.findByName("UNKNOWN")).isEmpty();
    }
}
