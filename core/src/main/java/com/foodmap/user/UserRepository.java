package com.foodmap.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface UserRepository extends JpaRepository<User, UUID> {

    /** Dùng được index {@code uq_users_email} vì điều kiện viết đúng dạng {@code LOWER(email)}. */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email) AND u.deletedAt IS NULL")
    Optional<User> findActiveByEmail(@Param("email") String email);

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);
}
