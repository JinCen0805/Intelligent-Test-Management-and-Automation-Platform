package com.testpilot.platform.user;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.testpilot.platform.common.persistence.MybatisRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository extends MybatisRepository<User, Long> {

    public UserRepository(UserMapper mapper) {
        super(mapper, User.class, User::getId);
    }

    public Optional<User> findByUsername(String username) {
        return findOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
    }

    public Optional<User> findByUsernameAndStatus(String username, String status) {
        return findOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, username)
                .eq(User::getStatus, status));
    }

    public Optional<User> findByIdAndStatus(Long id, String status) {
        return findOne(Wrappers.<User>lambdaQuery()
                .eq(User::getId, id)
                .eq(User::getStatus, status));
    }

    public List<User> findAllByStatusOrderByDisplayNameAsc(String status) {
        return list(Wrappers.<User>lambdaQuery()
                .eq(User::getStatus, status)
                .orderByAsc(User::getDisplayName));
    }

    public List<User> findAllByOrderByCreatedAtDesc() {
        return list(Wrappers.<User>lambdaQuery().orderByDesc(User::getCreatedAt));
    }
}
