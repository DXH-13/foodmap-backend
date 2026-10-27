package com.foodmap.place;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Luật giờ mở cửa, gồm khoảng qua nửa đêm. 2026-10-26 là thứ Hai. */
class OpeningHoursTests {

    private static final List<OpeningHours.Range> HOURS = List.of(
            new OpeningHours.Range(1, LocalTime.of(6, 0), LocalTime.of(10, 30), false),
            new OpeningHours.Range(1, LocalTime.of(18, 0), LocalTime.of(2, 0), false),   // qua nửa đêm
            new OpeningHours.Range(2, null, null, true),
            new OpeningHours.Range(3, LocalTime.of(7, 0), LocalTime.of(9, 0), false));

    private static LocalDateTime at(int day, int hour, int minute) {
        return LocalDateTime.of(2026, 10, 25, hour, minute).plusDays(day);
    }

    @Test
    void isOpen() {
        assertThat(OpeningHours.isOpen(HOURS, at(1, 6, 0))).isTrue();
        assertThat(OpeningHours.isOpen(HOURS, at(1, 10, 30))).isFalse();
        assertThat(OpeningHours.isOpen(HOURS, at(1, 23, 0))).isTrue();
        assertThat(OpeningHours.isOpen(HOURS, at(2, 1, 59))).isTrue();   // phần sau nửa đêm của thứ Hai
        assertThat(OpeningHours.isOpen(HOURS, at(2, 2, 0))).isFalse();
        assertThat(OpeningHours.isOpen(HOURS, at(2, 12, 0))).isFalse();
    }

    @Test
    void nextOpen() {
        assertThat(OpeningHours.nextOpen(HOURS, at(1, 12, 0))).contains(at(1, 18, 0));
        assertThat(OpeningHours.nextOpen(HOURS, at(2, 3, 0))).contains(at(3, 7, 0));
        assertThat(OpeningHours.nextOpen(HOURS, at(3, 9, 0))).contains(at(8, 6, 0));   // thứ Hai tuần sau
        assertThat(OpeningHours.nextOpen(List.of(), at(1, 0, 0))).isEmpty();
    }
}
