package com.foodmap.identity.infrastructure;

import com.foodmap.identity.domain.User;
import com.foodmap.identity.api.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Truy vấn bảng {@code users}.
 *
 * <p>Mọi truy vấn đều loại người dùng đã xoá mềm ({@code deleted_at IS NOT NULL}) —
 * tài khoản đã xoá coi như không tồn tại: không đăng nhập được, không đăng ký lại
 * trùng email được chặn (unique index chỉ áp dụng cho dòng chưa xoá).
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    /**
     * Tìm cho trang quản trị: {@code query} khớp một phần email hoặc tên hiển thị
     * (không phân biệt hoa thường), {@code role} lọc theo vai trò. Tham số null
     * nghĩa là không lọc theo điều đó.
     */
    @Query("""
            SELECT u FROM User u
            WHERE u.deletedAt IS NULL
              AND (:query IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
                  OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:role IS NULL OR u.role = :role)
            """)
    Page<User> search(@Param("query") String query,
                      @Param("role") UserRole role,
                      Pageable pageable);
}
