package com.utefit.service;

import com.utefit.enums.MembershipDurationUnit;
import com.utefit.util.MembershipTerm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class MembershipTermTest {
    @ParameterizedTest
    @CsvSource({
        "2026-01-31,1,MONTH,0,DAY,2026-02-27",
        "2024-01-31,1,MONTH,0,DAY,2024-02-28",
        "2024-02-29,1,YEAR,0,DAY,2025-02-27",
        "2023-03-01,1,YEAR,0,DAY,2024-02-29",
        "2026-01-31,3,MONTH,2,WEEK,2026-05-13",
        "2026-01-31,1,YEAR,1,MONTH,2027-02-27",
        "2026-10-26,2,WEEK,0,DAY,2026-11-08",
        "2026-10-26,7,DAY,0,DAY,2026-11-01"
    })
    void addsCalendarBaseThenBonusWithInclusiveEnd(LocalDate start, int quantity, MembershipDurationUnit unit,
                                                  int bonus, MembershipDurationUnit bonusUnit, LocalDate end) {
        var term = new MembershipTerm(quantity, unit, bonus, bonusUnit);
        assertEquals(end, term.endDate(start));
        assertEquals(java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1, term.daysAt(start));
    }

    @Test void rejectsInvalidTermsAndRetainsLegacyDaySemantics() {
        assertThrows(IllegalArgumentException.class, () -> new MembershipTerm(0, MembershipDurationUnit.MONTH, 0, MembershipDurationUnit.DAY));
        assertThrows(IllegalArgumentException.class, () -> new MembershipTerm(1, MembershipDurationUnit.YEAR, -1, MembershipDurationUnit.DAY));
        LocalDate start = LocalDate.of(2026, 11, 25);
        assertEquals(LocalDate.of(2026, 12, 24), MembershipTerm.legacy(30).endDate(start));
        assertEquals(29, new MembershipTerm(1, MembershipDurationUnit.MONTH, 0, MembershipDurationUnit.DAY).daysAt(LocalDate.of(2024, 2, 1)));
    }
}
