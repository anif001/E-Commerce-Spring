package com.ecommerce.repository;

import com.ecommerce.enums.Role;
import com.ecommerce.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User createTestUser(String email) {
        User user = User.builder()
                .name("Test User")
                .email(email)
                .password("encodedPassword")
                .role(Role.CUSTOMER)
                .build();
        return entityManager.persistFlushFind(user);
    }

    @Test
    void findByEmail_ShouldReturnUser_WhenExists() {
        User saved = createTestUser("test@example.com");

        Optional<User> found = userRepository.findByEmail("test@example.com");

        assertTrue(found.isPresent());
        assertEquals("Test User", found.get().getName());
        assertEquals("test@example.com", found.get().getEmail());
    }

    @Test
    void findByEmail_ShouldReturnEmpty_WhenNotExists() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        assertFalse(found.isPresent());
    }

    @Test
    void existsByEmail_ShouldReturnTrue_WhenExists() {
        createTestUser("exists@example.com");

        assertTrue(userRepository.existsByEmail("exists@example.com"));
    }

    @Test
    void existsByEmail_ShouldReturnFalse_WhenNotExists() {
        assertFalse(userRepository.existsByEmail("notfound@example.com"));
    }

    @Test
    void findByRole_ShouldReturnUsersWithMatchingRole() {
        createTestUser("customer1@example.com");
        createTestUser("customer2@example.com");
        User admin = User.builder()
                .name("Admin")
                .email("admin@example.com")
                .password("encodedPassword")
                .role(Role.ADMIN)
                .build();
        entityManager.persistFlushFind(admin);

        List<User> customers = userRepository.findByRole(Role.CUSTOMER);
        List<User> admins = userRepository.findByRole(Role.ADMIN);

        assertEquals(2, customers.size());
        assertEquals(1, admins.size());
    }

    @Test
    void save_ShouldPersistUser_WithTimestamps() {
        User user = User.builder()
                .name("New User")
                .email("new@example.com")
                .password("encodedPassword")
                .role(Role.SELLER)
                .build();

        User saved = userRepository.save(user);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals("New User", saved.getName());
    }

    @Test
    void delete_ShouldRemoveUser() {
        User saved = createTestUser("delete@example.com");

        userRepository.delete(saved);

        assertFalse(userRepository.findByEmail("delete@example.com").isPresent());
    }
}
