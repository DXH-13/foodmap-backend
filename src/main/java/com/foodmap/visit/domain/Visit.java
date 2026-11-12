package com.foodmap.visit.domain;

import com.foodmap.common.GeoUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Point;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Một lượt đến (check-in) của một người dùng tại một địa điểm.
 *
 * <p>Không extends {@code BaseEntity}: bảng {@code visits} không có cột
 * {@code updated_at} nên entity này tự khai {@code id} và {@code created_at}.
 * Chỉ tham chiếu tới người dùng và địa điểm bằng mã định danh.
 *
 * <p>{@code visitDate} là ngày theo giờ {@code Asia/Ho_Chi_Minh}, tính lúc ghi
 * bản ghi — không phải UTC. Unique index trên bảng ép luật tối đa 1 lượt /
 * người / địa điểm / ngày.
 */
@Entity
@Table(name = "visits", schema = "visit")
@EntityListeners(AuditingEntityListener.class)
public class Visit {

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "place_id", nullable = false)
    private UUID placeId;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    /**
     * Vị trí lúc ghi nhận. Dựng bằng {@link GeoUtils#toPoint} — đừng tự gọi
     * {@code new Coordinate(...)}.
     */
    @Column(name = "recorded_location", columnDefinition = "geography(Point,4326)", nullable = false)
    private Point recordedLocation;

    @Column(name = "distance_meters", nullable = false)
    private int distanceMeters;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Visit() {
    }

    public Visit(UUID userId, UUID placeId, LocalDate visitDate,
                 Point recordedLocation, int distanceMeters) {
        this.userId = userId;
        this.placeId = placeId;
        this.visitDate = visitDate;
        this.recordedLocation = recordedLocation;
        this.distanceMeters = distanceMeters;
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

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public Point getRecordedLocation() {
        return recordedLocation;
    }

    public int getDistanceMeters() {
        return distanceMeters;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
