package com.foodmap.place;

import com.foodmap.common.PageResponse;
import com.foodmap.common.exception.BadRequestException;
import com.foodmap.config.props.FoodmapProperties;
import com.foodmap.place.dto.PlaceSummaryDto;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final FoodmapProperties.Geo geo;

    public PlaceService(PlaceRepository placeRepository, FoodmapProperties properties) {
        this.placeRepository = placeRepository;
        this.geo = properties.geo();
    }

    /**
     * Tìm địa điểm quanh một toạ độ.
     *
     * <p>Bán kính ngoài khoảng cho phép trả về lỗi thay vì bị cắt bớt âm thầm
     * (FR-PLACE-02): người gọi cần biết yêu cầu của họ không được thực hiện như đã yêu cầu.
     */
    @Transactional(readOnly = true)
    public PageResponse<PlaceSummaryDto> searchNearby(double latitude,
                                                      double longitude,
                                                      Integer radiusMeters,
                                                      Pageable pageable) {

        int radius = radiusMeters == null ? geo.defaultRadiusMeters() : radiusMeters;

        if (radius < geo.minRadiusMeters() || radius > geo.maxRadiusMeters()) {
            throw new BadRequestException(
                    "RADIUS_OUT_OF_RANGE",
                    "place.error.radius_out_of_range",
                    geo.minRadiusMeters(), geo.maxRadiusMeters());
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new BadRequestException("INVALID_COORDINATES", "place.error.invalid_coordinates");
        }

        String locale = LocaleContextHolder.getLocale().getLanguage();

        return PageResponse.from(
                placeRepository.findNearby(latitude, longitude, radius, locale, pageable),
                PlaceSummaryDto::from);
    }
}
