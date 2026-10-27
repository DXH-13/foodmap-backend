package com.foodmap.visit.web;

import com.foodmap.auth.AuthPrincipal;
import com.foodmap.common.PageResponse;
import com.foodmap.visit.VisitService;
import com.foodmap.visit.dto.RecordVisitRequest;
import com.foodmap.visit.dto.VisitDto;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "visit", description = "Ghi nhận lượt đến")
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    @PostMapping("/api/v1/places/{placeId}/visits")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            operationId = "recordVisit",
            summary = "Ghi nhận đã đến một địa điểm",
            description = """
                    Yêu cầu toạ độ hiện tại nằm trong 200m quanh địa điểm, và tối đa
                    1 lượt / người / địa điểm / ngày theo giờ Asia/Ho_Chi_Minh.
                    Mã lỗi: VISIT_TOO_FAR · VISIT_ALREADY_TODAY (409).""")
    public VisitDto recordVisit(@AuthenticationPrincipal AuthPrincipal principal,
                                @PathVariable UUID placeId,
                                @Valid @RequestBody RecordVisitRequest request) {
        return visitService.record(principal.userId(), placeId,
                request.coordinates().latitude(), request.coordinates().longitude());
    }

    @GetMapping("/api/v1/me/visits")
    @Operation(operationId = "listOwnVisits", summary = "Lấy lịch sử đã đến của chính mình")
    public PageResponse<VisitDto> listOwnVisits(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return visitService.listOwn(principal.userId(), PageRequest.of(page, size));
    }
}
