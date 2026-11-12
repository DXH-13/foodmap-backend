package com.foodmap.chatbot.application;

import com.foodmap.common.GeoUtils;
import com.foodmap.place.api.PlaceApi;
import com.foodmap.place.api.PlaceSummary;
import com.foodmap.review.api.ReviewApi;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Các tool nội bộ mà mô hình ngôn ngữ được phép gọi. Đây là đường duy nhất
 * model chạm tới dữ liệu FoodMap — mọi tool đều gọi qua {@code api/} công khai
 * của module sở hữu dữ liệu, không bao giờ đọc thẳng bảng của module khác.
 *
 * <p>Nguyên tắc chống bịa (yêu cầu trong hợp đồng): model chỉ được nhắc tới
 * địa điểm có trong kết quả tool trả về. {@link ChatService} thu thập mọi id
 * từ {@link ToolOutcome#placeIds()} để phát sự kiện PLACE_REFERENCES và lưu
 * vào tin nhắn trả lời.
 */
@Component
public class ChatTools {

    /** Số kết quả tối đa mặc định của tool tìm kiếm. */
    private static final int DEFAULT_LIMIT = 5;

    private final PlaceApi places;
    private final ReviewApi reviews;

    public ChatTools(PlaceApi places, ReviewApi reviews) {
        this.places = places;
        this.reviews = reviews;
    }

    /** Bối cảnh của một lượt chat: vị trí người dùng (nếu có) và ngôn ngữ. */
    public record ToolContext(Double latitude, Double longitude, String locale) {
    }

    /** Kết quả thực thi một tool: dữ liệu trả cho model + id các quán liên quan. */
    public record ToolOutcome(Map<String, Object> response, List<UUID> placeIds) {
    }

    /** Khai báo 3 tool cho model (đúng định dạng functionDeclarations của Gemini). */
    public List<ToolDefinition> definitions() {
        return List.of(
                new ToolDefinition(
                        "search_places",
                        "Tìm địa điểm ăn uống. Dùng khi người dùng hỏi gợi ý quán, hỏi quán quanh đây, "
                                + "hoặc hỏi về một món ăn / khu vực. Ưu tiên dùng latitude/longitude khi có "
                                + "tọa độ người dùng; không có tọa độ thì dùng query (tên món, tên quán, khu vực).",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "query", Map.of(
                                                "type", "string",
                                                "description", "Từ khoá: tên món, tên quán hoặc khu vực, ví dụ 'phở', 'Quận 1'"),
                                        "latitude", Map.of(
                                                "type", "number",
                                                "description", "Vĩ độ của người dùng, khi được cung cấp"),
                                        "longitude", Map.of(
                                                "type", "number",
                                                "description", "Kinh độ của người dùng, khi được cung cấp"),
                                        "radiusMeters", Map.of(
                                                "type", "integer",
                                                "description", "Bán kính tìm kiếm theo mét, mặc định 2000"),
                                        "limit", Map.of(
                                                "type", "integer",
                                                "description", "Số kết quả tối đa, mặc định 5")),
                                "required", List.of())),
                new ToolDefinition(
                        "get_place_details",
                        "Lấy thông tin chi tiết của một địa điểm đã biết id (từ kết quả của search_places): "
                                + "tên, địa chỉ, điểm trung bình, số đánh giá, số lượt đến.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "placeId", Map.of(
                                                "type", "string",
                                                "description", "Id (UUID) của địa điểm")),
                                "required", List.of("placeId"))),
                new ToolDefinition(
                        "list_place_reviews",
                        "Đọc các đánh giá đã duyệt mới nhất của một địa điểm, để trả lời câu hỏi "
                                + "kiểu 'quán này có ngon không', 'mọi người nhận xét quán này thế nào'.",
                        Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "placeId", Map.of(
                                                "type", "string",
                                                "description", "Id (UUID) của địa điểm"),
                                        "limit", Map.of(
                                                "type", "integer",
                                                "description", "Số đánh giá tối đa, mặc định 3")),
                                "required", List.of("placeId"))));
    }

    /**
     * Thực thi một lời gọi tool. Lỗi nghiệp vụ (ví dụ bán kính ngoài khoảng,
     * địa điểm không tồn tại) được trả lại cho model dưới dạng
     * {@code {"error": "..."}} thay vì làm đổ cả lượt chat — model tự quyết
     * định trả lời tiếp thế nào (thường là hỏi lại hoặc nói không tìm thấy).
     */
    public ToolOutcome execute(FunctionCall call, ToolContext context) {
        try {
            return switch (call.name()) {
                case "search_places" -> searchPlaces(call.args(), context);
                case "get_place_details" -> getPlaceDetails(call.args(), context);
                case "list_place_reviews" -> listPlaceReviews(call.args());
                default -> new ToolOutcome(Map.of("error", "unknown tool: " + call.name()), List.of());
            };
        } catch (RuntimeException ex) {
            return new ToolOutcome(Map.of("error", ex.getMessage()), List.of());
        }
    }

    private ToolOutcome searchPlaces(Map<String, Object> args, ToolContext context) {
        int limit = intArg(args, "limit", DEFAULT_LIMIT);
        List<PlaceSummary> found;
        Double lat = numberArg(args, "latitude");
        Double lng = numberArg(args, "longitude");
        if (lat != null && lng != null) {
            Integer radius = intArgOrNull(args, "radiusMeters");
            found = places.searchNearby(lat, lng, radius, limit);
        } else if (args.get("query") instanceof String query && !query.isBlank()) {
            found = places.searchByKeyword(query, limit);
        } else {
            return new ToolOutcome(
                    Map.of("places", List.of(),
                            "note", "Cần tọa độ người dùng hoặc từ khóa để tìm kiếm."),
                    List.of());
        }
        List<Map<String, Object>> placeJson = new ArrayList<>();
        List<UUID> ids = new ArrayList<>();
        for (PlaceSummary place : found) {
            ids.add(place.id());
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("id", place.id().toString());
            json.put("name", place.name());
            json.put("placeType", place.placeType());
            json.put("address", place.address());
            json.put("averageRating", place.averageRating());
            json.put("reviewCount", place.reviewCount());
            json.put("visitCount", place.visitCount());
            json.put("latitude", place.latitude());
            json.put("longitude", place.longitude());
            if (context.latitude() != null && context.longitude() != null) {
                json.put("distanceMeters", Math.round(GeoUtils.distanceMeters(
                        context.latitude(), context.longitude(), place.latitude(), place.longitude())));
            }
            placeJson.add(json);
        }
        return new ToolOutcome(Map.of("places", placeJson), ids);
    }

    private ToolOutcome getPlaceDetails(Map<String, Object> args, ToolContext context) {
        UUID placeId = uuidArg(args, "placeId");
        if (placeId == null) {
            return new ToolOutcome(Map.of("error", "placeId không hợp lệ."), List.of());
        }
        List<PlaceSummary> found = places.getSummaries(List.of(placeId), context.locale());
        if (found.isEmpty()) {
            return new ToolOutcome(Map.of("error", "Không tìm thấy địa điểm."), List.of());
        }
        PlaceSummary place = found.getFirst();
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", place.id().toString());
        json.put("name", place.name());
        json.put("placeType", place.placeType());
        json.put("address", place.address());
        json.put("averageRating", place.averageRating());
        json.put("reviewCount", place.reviewCount());
        json.put("visitCount", place.visitCount());
        json.put("latitude", place.latitude());
        json.put("longitude", place.longitude());
        if (context.latitude() != null && context.longitude() != null) {
            json.put("distanceMeters", Math.round(GeoUtils.distanceMeters(
                    context.latitude(), context.longitude(), place.latitude(), place.longitude())));
        }
        return new ToolOutcome(Map.of("place", json), List.of(place.id()));
    }

    private ToolOutcome listPlaceReviews(Map<String, Object> args) {
        UUID placeId = uuidArg(args, "placeId");
        if (placeId == null) {
            return new ToolOutcome(Map.of("error", "placeId không hợp lệ."), List.of());
        }
        int limit = intArg(args, "limit", 3);
        List<Map<String, Object>> reviewJson = reviews.listApprovedReviews(placeId, limit).stream()
                .map(review -> {
                    Map<String, Object> json = new LinkedHashMap<>();
                    json.put("rating", review.rating());
                    json.put("content", review.content());
                    json.put("createdAt", review.createdAt() == null ? null : review.createdAt().toString());
                    return json;
                })
                .toList();
        return new ToolOutcome(Map.of("reviews", reviewJson), List.of(placeId));
    }

    private static Double numberArg(Map<String, Object> args, String name) {
        return args.get(name) instanceof Number number ? number.doubleValue() : null;
    }

    private static int intArg(Map<String, Object> args, String name, int defaultValue) {
        Integer value = intArgOrNull(args, name);
        return value == null ? defaultValue : value;
    }

    private static Integer intArgOrNull(Map<String, Object> args, String name) {
        return args.get(name) instanceof Number number ? number.intValue() : null;
    }

    private static UUID uuidArg(Map<String, Object> args, String name) {
        try {
            return args.get(name) instanceof String value ? UUID.fromString(value) : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** Giữ thứ tự xuất hiện, bỏ trùng — để PLACE_REFERENCES không lặp id. */
    static List<UUID> deduplicate(List<UUID> ids) {
        return List.copyOf(new LinkedHashSet<>(ids));
    }
}
