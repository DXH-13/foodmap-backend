package com.foodmap.visit.web;

import com.foodmap.common.PageResponse;
import com.foodmap.identity.api.AuthPrincipal;
import com.foodmap.visit.application.VisitService;
import com.foodmap.visit.web.dto.RecordVisitRequest;
import com.foodmap.visit.web.dto.VisitDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Ghé thăm. Khớp các operation {@code recordVisit} và {@code listOwnVisits}
 * trong hợp đồng.
 */
@RestController
@RequestMapping("/app/v1")
@Tag(name = "visit", description = "Lịch sử ghé thăm")
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    @PostMapping("/places/{placeId}/visits")
    @Operation(operationId = "recordVisit", summary = "Ghi nhận đã đến một địa điểm")
    public ResponseEntity<VisitDto> recordVisit(@PathVariable UUID placeId,
                                                @AuthenticationPrincipal AuthPrincipal principal,
                                                @Valid @RequestBody RecordVisitRequest request) {
        VisitDto created = visitService.recordVisit(placeId, principal.userId(), request);
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping("/me/visits")
    @Operation(operationId = "listOwnVisits", summary = "Lấy lịch sử đã đến của chính mình")
    public PageResponse<VisitDto> listOwnVisits(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return visitService.listOwnVisits(principal.userId(), page, size);
    }
}
