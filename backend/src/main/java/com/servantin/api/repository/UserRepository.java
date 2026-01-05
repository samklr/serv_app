package com.servantin.api.repository;

import com.servantin.api.domain.entity.User;
import com.servantin.api.domain.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(UserRole role);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.providerProfile WHERE u.email = :email")
    Optional<User> findByEmailWithProfile(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.providerProfile WHERE u.id = :id")
    Optional<User> findByIdWithProfile(UUID id);
}
