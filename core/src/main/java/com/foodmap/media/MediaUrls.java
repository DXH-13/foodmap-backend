package com.foodmap.media;

import com.foodmap.config.props.FoodmapProperties;
import org.springframework.stereotype.Component;

/**
 * Dựng URL công khai từ khoá object. CSDL chỉ lưu khoá — đổi CDN/bucket không phải migrate dữ liệu.
 */
@Component
public class MediaUrls {

    private final String prefix;

    public MediaUrls(FoodmapProperties properties) {
        FoodmapProperties.Storage storage = properties.storage();
        this.prefix = storage.endpoint().replaceAll("/+$", "") + "/" + storage.bucket() + "/";
    }

    /** {@code null} khi không có khoá — field ảnh trong hợp đồng là nullable. */
    public String of(String key) {
        return key == null ? null : prefix + key;
    }
}
