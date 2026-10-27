package com.foodmap.user;

import com.foodmap.common.exception.ConflictException;
import com.foodmap.common.exception.UnauthorizedException;
import org.springframework.dao.DataIntegrityViolationException;
import com.foodmap.user.dto.UserProfileDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/** Cửa vào duy nhất của feature khác tới dữ liệu người dùng. */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Người dùng chưa bị xoá mềm, so email không phân biệt hoa thường. */
    @Transactional(readOnly = true)
    public Optional<User> findActiveByEmail(String email) {
        return userRepository.findActiveByEmail(email);
    }

    /**
     * Tạo người dùng mới với vai trò {@code USER}, chưa xác minh email.
     * Email trùng (không phân biệt hoa thường) → 409 {@code EMAIL_ALREADY_EXISTS}.
     * Hai request cùng email chạy song song thì index {@code uq_users_email} chặn request sau.
     */
    @Transactional
    public User register(String email, String passwordHash, String displayName, String preferredLocale) {
        if (userRepository.findActiveByEmail(email).isPresent()) {
            throw emailAlreadyExists();
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setDisplayName(displayName);
        user.setPreferredLocale(preferredLocale);
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw emailAlreadyExists();
        }
    }

    private static ConflictException emailAlreadyExists() {
        return new ConflictException("EMAIL_ALREADY_EXISTS", "auth.error.email_already_exists");
    }

    @Transactional(readOnly = true)
    public Optional<User> findActiveById(UUID id) {
        return userRepository.findByIdAndDeletedAtIsNull(id);
    }

    /**
     * Hồ sơ của người đang đăng nhập. Tài khoản đã bị xoá mềm mà token còn hạn thì coi như
     * phiên không hợp lệ (401), không phải 404 — client chỉ cần đăng nhập lại.
     */
    @Transactional(readOnly = true)
    public UserProfileDto getProfile(UUID id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .map(UserProfileDto::from)
                .orElseThrow(() -> new UnauthorizedException("INVALID_TOKEN", "auth.error.invalid_token"));
    }
}
