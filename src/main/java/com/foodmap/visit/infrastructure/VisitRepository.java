package com.foodmap.visit.infrastructure;

import com.foodmap.visit.domain.Visit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface VisitRepository extends JpaRepository<Visit, UUID> {

    /** Đã ghi nhận lượt đến cho địa điểm này trong ngày này chưa (chống spam 1/ngày). */
    boolean existsByUserIdAndPlaceIdAndVisitDate(UUID userId, UUID placeId, LocalDate visitDate);

    Page<Visit> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByPlaceId(UUID placeId);

    @Query("SELECT COUNT(DISTINCT v.userId) FROM Visit v WHERE v.placeId = :placeId")
    long countDistinctUserIdByPlaceId(@Param("placeId") UUID placeId);
}
