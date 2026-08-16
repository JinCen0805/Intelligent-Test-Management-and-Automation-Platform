package com.testpilot.platform.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameAndStatus(String username, String status);
    Optional<User> findByIdAndStatus(Long id, String status);
}
