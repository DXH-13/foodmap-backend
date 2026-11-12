package com.foodmap.favorite.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Một địa điểm trong danh sách yêu thích của một người dùng.
 *
 * <p>Không extends {@code BaseEntity}: bảng {@code favorites} không có cột
 * {@code updated_at} nên entity này tự khai {@code id} và {@code created_at}.
 * Chỉ tham chiếu tới người dùng và địa điểm bằng mã định danh.
 */
@Entity
@Table(name = "favorites", schema = "favorite")
@EntityListeners(AuditingEntityListener.class)
public class Favorite {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "place_id", nullable = false)
    private UUID placeId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Favorite() {
    }

    public Favorite(UUID userId, UUID placeId) {
        this.userId = userId;
        this.placeId = placeId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getPlaceId() {
        return placeId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
