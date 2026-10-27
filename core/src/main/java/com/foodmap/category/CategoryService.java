package com.foodmap.category;

import com.foodmap.category.dto.CategoryDto;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Danh mục món ăn. Chỉ đọc nên truy vấn thẳng bằng SQL, không cần entity.
 * Tên lấy theo locale, thiếu thì lùi về bản tiếng Việt (FR-I18N-03).
 */
@Service
public class CategoryService {

    private static final String SELECT = """
            SELECT c.id, c.slug, COALESCE(t.name, tvi.name) AS name, c.icon, c.display_order
            FROM categories c
            LEFT JOIN category_translations t   ON t.category_id = c.id   AND t.locale = :locale
            LEFT JOIN category_translations tvi ON tvi.category_id = c.id AND tvi.locale = 'vi'
            """;

    private final JdbcClient jdbc;

    public CategoryService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> listAll() {
        return jdbc.sql(SELECT + "ORDER BY c.display_order, c.slug")
                .param("locale", locale())
                .query((rs, n) -> map(rs))
                .list();
    }

    /** Danh mục của từng địa điểm, theo {@code display_order}. Địa điểm không có danh mục thì vắng mặt trong map. */
    @Transactional(readOnly = true)
    public Map<UUID, List<CategoryDto>> findByPlaceIds(Collection<UUID> placeIds) {
        if (placeIds.isEmpty()) {
            return Map.of();
        }
        record Row(UUID placeId, CategoryDto category) {}
        return jdbc.sql("""
                        SELECT pc.place_id, x.* FROM place_categories pc
                        JOIN (%s) x ON x.id = pc.category_id
                        WHERE pc.place_id IN (:placeIds)
                        ORDER BY x.display_order, x.slug
                        """.formatted(SELECT))
                .param("locale", locale())
                .param("placeIds", placeIds)
                .query((rs, n) -> new Row(rs.getObject("place_id", UUID.class), map(rs)))
                .list()
                .stream()
                .collect(Collectors.groupingBy(Row::placeId, LinkedHashMap::new,
                        Collectors.mapping(Row::category, Collectors.toList())));
    }

    private static CategoryDto map(ResultSet rs) throws SQLException {
        return new CategoryDto(
                rs.getObject("id", UUID.class),
                rs.getString("slug"),
                rs.getString("name"),
                rs.getString("icon"),
                rs.getInt("display_order"));
    }

    private static String locale() {
        return LocaleContextHolder.getLocale().getLanguage();
    }
}
