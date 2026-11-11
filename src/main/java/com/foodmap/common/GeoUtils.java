package com.foodmap.common;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * Chuyển đổi giữa (vĩ độ, kinh độ) của API và {@link Point} của PostGIS.
 *
 * <p><b>Đây là nơi duy nhất được phép gọi {@code new Coordinate(...)} cho toạ độ địa lý.</b>
 * PostGIS nhận <b>kinh độ trước, vĩ độ sau</b> — ngược với cách người ta thường đọc.
 * Rải phép chuyển đổi này khắp codebase là bảo đảm sẽ có chỗ viết nhầm thứ tự,
 * và điểm sẽ rơi ra giữa đại dương. Xem docs/SDD/du-lieu/geo-model.md.
 */
public final class GeoUtils {

    /** WGS-84 — hệ toạ độ GPS và mọi API bản đồ đều dùng. */
    public static final int SRID_WGS84 = 4326;

    private static final GeometryFactory FACTORY =
            new GeometryFactory(new PrecisionModel(), SRID_WGS84);

    // Khung bao Việt Nam — bắt được phần lớn trường hợp đảo nhầm lat/lng.
    private static final double VN_MIN_LAT = 8.0;
    private static final double VN_MAX_LAT = 23.5;
    private static final double VN_MIN_LNG = 102.0;
    private static final double VN_MAX_LNG = 110.0;

    private GeoUtils() {
    }

    /**
     * @param latitude  vĩ độ, −90…90
     * @param longitude kinh độ, −180…180
     */
    public static Point toPoint(double latitude, double longitude) {
        // Coordinate(x, y) = (kinh độ, vĩ độ) — thứ tự này là nguồn lỗi số một.
        Point point = FACTORY.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(SRID_WGS84);
        return point;
    }

    public static double latitudeOf(Point point) {
        return point.getY();
    }

    public static double longitudeOf(Point point) {
        return point.getX();
    }

    /**
     * Khoảng cách (mét) giữa hai toạ độ, công thức Haversine trên mặt cầu.
     *
     * <p>Đủ chính xác cho việc sắp xếp (ví dụ yêu thích theo khoảng cách). Khi cần
     * độ chính xác tuyệt đối để ra quyết định (ví dụ check-in trong 200m), dùng
     * PostGIS {@code ST_Distance} trên cột geography thay vì hàm này.
     */
    public static double distanceMeters(double latitude1, double longitude1,
                                        double latitude2, double longitude2) {
        final double earthRadiusMeters = 6_371_000;
        double dLat = Math.toRadians(latitude2 - latitude1);
        double dLng = Math.toRadians(longitude2 - longitude1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latitude1)) * Math.cos(Math.toRadians(latitude2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * earthRadiusMeters * Math.asin(Math.sqrt(a));
    }

    /** Toạ độ có nằm trong khung bao Việt Nam không. Dùng để cảnh báo dữ liệu nghi vấn. */
    public static boolean isWithinVietnam(double latitude, double longitude) {
        return latitude >= VN_MIN_LAT && latitude <= VN_MAX_LAT
                && longitude >= VN_MIN_LNG && longitude <= VN_MAX_LNG;
    }
}
