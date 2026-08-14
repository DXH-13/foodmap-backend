package com.foodmap.place;

import com.foodmap.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Địa điểm — quán ăn, hàng ăn, chợ đồ ăn, quán cà phê.
 *
 * <p>Tên và mô tả <b>không</b> nằm ở đây mà ở bảng {@code place_translations},
 * vì mỗi địa điểm có bản tiếng Việt (bắt buộc) và tiếng Anh (tuỳ chọn).
 *
 * <p>Bốn cột đếm ({@code averageRating}, {@code reviewCount}, {@code visitCount},
 * {@code distinctVisitorCount}) là <b>dữ liệu dẫn xuất</b>, tính sẵn để tránh truy vấn
 * tổng hợp ở mỗi lần đọc. Xem docs/04-data/erd.md để biết khi nào chúng được tính lại.
 */
@Entity
@Table(name = "places")
public class Place extends BaseEntity {

    @Column(nullable = false)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "place_type", nullable = false, length = 20)
    private PlaceType placeType;

    /**
     * Toạ độ. Dựng bằng {@code GeoUtils.toPoint(lat, lng)} — <b>đừng</b> tự gọi
     * {@code new Coordinate(...)}, thứ tự là (kinh độ, vĩ độ) và rất dễ viết ngược.
     */
    @Column(columnDefinition = "geography(Point,4326)", nullable = false)
    private Point location;

    /** Hàng rong và quầy chợ có thể không có địa chỉ chính thức. */
    @Column(length = 500)
    private String address;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PlaceStatus status = PlaceStatus.DRAFT;

    /** Được bật tự động khi có 3 báo cáo đóng cửa từ 3 người khác nhau (FR-FEEDBACK-05). */
    @Column(name = "needs_review", nullable = false)
    private boolean needsReview = false;

    /** {@code null} khi chưa có đánh giá nào — <b>không phải 0</b> (FR-PLACE-12). */
    @Column(name = "average_rating", precision = 2, scale = 1)
    private BigDecimal averageRating;

    @Column(name = "review_count", nullable = false)
    private int reviewCount = 0;

    /** Tổng số lượt đến, không phải số người khác nhau. */
    @Column(name = "visit_count", nullable = false)
    private long visitCount = 0;

    @Column(name = "distinct_visitor_count", nullable = false)
    private long distinctVisitorCount = 0;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public PlaceType getPlaceType() {
        return placeType;
    }

    public void setPlaceType(PlaceType placeType) {
        this.placeType = placeType;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public PlaceStatus getStatus() {
        return status;
    }

    public void setStatus(PlaceStatus status) {
        this.status = status;
    }

    public boolean isNeedsReview() {
        return needsReview;
    }

    public void setNeedsReview(boolean needsReview) {
        this.needsReview = needsReview;
    }

    public BigDecimal getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(BigDecimal averageRating) {
        this.averageRating = averageRating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public long getVisitCount() {
        return visitCount;
    }

    public void setVisitCount(long visitCount) {
        this.visitCount = visitCount;
    }

    public long getDistinctVisitorCount() {
        return distinctVisitorCount;
    }

    public void setDistinctVisitorCount(long distinctVisitorCount) {
        this.distinctVisitorCount = distinctVisitorCount;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
