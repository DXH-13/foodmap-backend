package com.foodmap.review.domain;

import com.foodmap.common.BaseEntity;
import com.foodmap.review.api.ReviewStatus;
import jakarta.persistence.Column;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Đánh giá của một người dùng cho một địa điểm.
 *
 * <p>Chỉ tham chiếu tới địa điểm và người dùng bằng mã định danh — không giữ
 * tham chiếu đối tượng sang entity của module khác (quy tắc ranh giới số 2).
 * Xoá là xoá mềm ({@code deleted_at}); unique index trên bảng cho phép gửi lại
 * đánh giá sau khi đã xoá bản cũ.
 */
@Entity
@Table(name = "reviews")
public class Review extends BaseEntity {

    @Column(name = "place_id", nullable = false)
    private UUID placeId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * 1..5, khớp CHECK trên bảng. Cột trong DB là SMALLINT nhưng Java vẫn dùng
     * int — {@code @JdbcTypeCode(SMALLINT)} ép kiểu JDBC về SMALLINT để
     * Hibernate validate pass. Chỉ khai {@code columnDefinition = "smallint"}
     * là KHÔNG đủ: Postgres báo tên kiểu là "int2" chứ không phải "smallint",
     * nên cả hai đường so sánh của Hibernate (mã kiểu INTEGER≠SMALLINT, tên
     * kiểu "smallint"≠"int2") đều trượt — đó là lý do CI bước 4 vẫn đỏ sau
     * lần sửa đầu.
     */
    @Column(nullable = false, columnDefinition = "smallint")
    @JdbcTypeCode(SqlTypes.SMALLINT)
    private int rating;

    @Column
    private String content;

    @Column(nullable = false, length = 5)
    private String locale = "vi";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReviewStatus status = ReviewStatus.PENDING;

    @Column(name = "moderation_note")
    private String moderationNote;

    @Column(name = "moderated_by")
    private UUID moderatedBy;

    @Column(name = "moderated_at")
    private Instant moderatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Review() {
    }

    private Review(UUID placeId, UUID userId, int rating, String content, String locale) {
        this.placeId = placeId;
        this.userId = userId;
        this.rating = rating;
        this.content = content;
        this.locale = locale;
    }

    public static Review create(UUID placeId, UUID userId, int rating, String content, String locale) {
        return new Review(placeId, userId, rating, content, locale);
    }

    /**
     * Gửi lại đánh giá: cập nhật nội dung và đưa về {@code PENDING} để kiểm
     * duyệt lại, bất kể trạng thái trước đó là gì (hợp đồng chỉ nêu rõ trường
     * hợp APPROVED → PENDING; REJECTED/HIDDEN sửa lại cũng phải qua kiểm duyệt
     * lại thì mới nhất quán). Quyết định kiểm duyệt cũ bị xoá theo.
     */
    public void resubmit(int rating, String content) {
        this.rating = rating;
        this.content = content;
        this.status = ReviewStatus.PENDING;
        this.moderationNote = null;
        this.moderatedBy = null;
        this.moderatedAt = null;
    }

    /** Áp dụng quyết định kiểm duyệt của moderator. */
    public void moderate(ReviewStatus status, String moderationNote, UUID moderatedBy) {
        this.status = status;
        this.moderationNote = moderationNote;
        this.moderatedBy = moderatedBy;
        this.moderatedAt = Instant.now();
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    public UUID getPlaceId() {
        return placeId;
    }

    public UUID getUserId() {
        return userId;
    }

    public int getRating() {
        return rating;
    }

    public String getContent() {
        return content;
    }

    public String getLocale() {
        return locale;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public String getModerationNote() {
        return moderationNote;
    }

    public UUID getModeratedBy() {
        return moderatedBy;
    }

    public Instant getModeratedAt() {
        return moderatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
