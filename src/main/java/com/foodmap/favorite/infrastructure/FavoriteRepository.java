package com.foodmap.favorite.infrastructure;

import com.foodmap.favorite.domain.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    boolean existsByUserIdAndPlaceId(UUID userId, UUID placeId);

    void deleteByUserIdAndPlaceId(UUID userId, UUID placeId);

    Page<Favorite> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /** Toàn bộ yêu thích của một người — dùng cho sắp xếp theo khoảng cách (số lượng nhỏ). */
    List<Favorite> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
