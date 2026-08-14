package com.foodmap.place;

/**
 * Loại địa điểm. Khớp constraint {@code places_type_check} trong V1__init_schema.sql
 * và enum {@code PlaceType} trong openapi.yaml.
 */
public enum PlaceType {

    /** Quán ăn có mặt bằng cố định, bàn ghế. */
    RESTAURANT,

    /** Hàng ăn vỉa hè, xe đẩy, gánh hàng rong. */
    STREET_FOOD,

    /** Chợ đồ ăn, food court — nhiều quầy trong một khu. */
    FOOD_MARKET,

    /** Quán cà phê. */
    CAFE
}
