package com.streamly.streamly.domain.user.repository;

import com.streamly.streamly.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 이메일로 기존 가입자인지 확인하기 위함
    Optional<User> findByEmail(String email);
}
