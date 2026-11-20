package com.testpilot.platform.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.testpilot.platform.rbac.RbacService;
import com.testpilot.platform.user.User;
import com.testpilot.platform.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminBootstrapTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private RbacService rbacService;
    private MockEnvironment environment;
    private DefaultApplicationArguments args;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        rbacService = mock(RbacService.class);
        environment = new MockEnvironment();
        args = new DefaultApplicationArguments(new String[0]);
    }

    @Test
    void createsAdminAndSyncsRbacWhenDatabaseIsEmpty() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode("strong-pass")).thenReturn("encoded-hash");

        bootstrap("admin", "strong-pass").run(args);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User created = captor.getValue();
        assertEquals("admin", created.getUsername());
        assertEquals("ADMIN", created.getRole());
        assertEquals("ENABLED", created.getStatus());
        assertEquals("encoded-hash", created.getPasswordHash());
        verify(rbacService).syncLegacyAssignments();
    }

    @Test
    void doesNothingWhenUsersAlreadyExist() {
        when(userRepository.count()).thenReturn(1L);

        bootstrap("admin", "strong-pass").run(args);

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(rbacService);
    }

    @Test
    void skipsQuietlyWhenCredentialsMissingInDevProfile() {
        environment.setActiveProfiles("local");
        when(userRepository.count()).thenReturn(0L);

        bootstrap("", "").run(args);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void failsFastWhenCredentialsMissingOutsideDevProfiles() {
        when(userRepository.count()).thenReturn(0L);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> bootstrap("", "").run(args)
        );
        assertTrue(exception.getMessage().contains("ADMIN_INITIAL_USERNAME"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void rejectsTooShortPassword() {
        when(userRepository.count()).thenReturn(0L);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> bootstrap("admin", "short").run(args)
        );
        assertTrue(exception.getMessage().contains("8"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void toleratesConcurrentInsertOfSameAdmin() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.save(any(User.class))).thenThrow(new DuplicateKeyException("duplicate"));
        when(passwordEncoder.encode("strong-pass")).thenReturn("encoded-hash");

        bootstrap("admin", "strong-pass").run(args);

        verify(userRepository).save(any(User.class));
        verifyNoInteractions(rbacService);
    }

    private AdminBootstrap bootstrap(String username, String password) {
        return new AdminBootstrap(userRepository, passwordEncoder, rbacService, environment, username, password);
    }
}
