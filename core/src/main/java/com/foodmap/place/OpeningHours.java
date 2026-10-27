package com.foodmap.place;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Tính "đang mở cửa" và "mở cửa lúc nào tiếp theo" từ giờ mở cửa, theo giờ địa phương.
 *
 * <p>Khoảng có {@code open >= close} là <b>qua nửa đêm</b> (ví dụ 18:00–02:00): phần sau
 * nửa đêm thuộc về ngày hôm sau. Luật này phải khớp {@code PlaceRepository.NEARBY_FILTER}.
 */
final class OpeningHours {

    /** Một khoảng giờ mở cửa; {@code open}/{@code close} là {@code null} khi nghỉ cả ngày. */
    record Range(int dayOfWeek, LocalTime open, LocalTime close, boolean closedAllDay) {
    }

    private OpeningHours() {
    }

    static boolean isOpen(List<Range> ranges, LocalDateTime now) {
        int today = now.getDayOfWeek().getValue();
        int yesterday = now.minusDays(1).getDayOfWeek().getValue();
        LocalTime t = now.toLocalTime();
        return ranges.stream().filter(r -> !r.closedAllDay()).anyMatch(r -> {
            boolean overnight = !r.open().isBefore(r.close());
            if (r.dayOfWeek() == today && !t.isBefore(r.open()) && (overnight || t.isBefore(r.close()))) {
                return true;
            }
            return r.dayOfWeek() == yesterday && overnight && t.isBefore(r.close());
        });
    }

    /** Lần mở cửa sớm nhất sau {@code now} trong 7 ngày tới; rỗng khi không có giờ mở nào. */
    static Optional<LocalDateTime> nextOpen(List<Range> ranges, LocalDateTime now) {
        for (int d = 0; d <= 7; d++) {
            var date = now.toLocalDate().plusDays(d);
            Optional<LocalDateTime> next = ranges.stream()
                    .filter(r -> !r.closedAllDay() && r.dayOfWeek() == date.getDayOfWeek().getValue())
                    .map(r -> date.atTime(r.open()))
                    .filter(at -> at.isAfter(now))
                    .min(LocalDateTime::compareTo);
            if (next.isPresent()) {
                return next;
            }
        }
        return Optional.empty();
    }
}
