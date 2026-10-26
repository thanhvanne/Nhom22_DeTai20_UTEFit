package com.utefit.util;

import com.utefit.enums.MembershipDurationUnit;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record MembershipTerm(int quantity, MembershipDurationUnit unit, int bonusQuantity, MembershipDurationUnit bonusUnit) {
    public MembershipTerm {
        if (unit == null || bonusUnit == null || quantity < 1 || quantity > unit.getMaximum()
                || bonusQuantity < 0 || bonusQuantity > bonusUnit.getMaximum())
            throw new IllegalArgumentException("Thời hạn/thời gian thưởng không hợp lệ (tối đa 100 năm cho mỗi phần).");
    }

    public static MembershipTerm legacy(Integer days) {
        return new MembershipTerm(days == null ? 0 : days, MembershipDurationUnit.DAY, 0, MembershipDurationUnit.DAY);
    }

    public LocalDate endDate(LocalDate start) {
        return bonusUnit.addTo(unit.addTo(start, quantity), bonusQuantity).minusDays(1);
    }

    public int daysAt(LocalDate start) {
        return Math.toIntExact(ChronoUnit.DAYS.between(start, endDate(start)) + 1);
    }

    public String description() {
        return quantity + " " + unit.getLabel() + (bonusQuantity > 0 ? " + tặng " + bonusQuantity + " " + bonusUnit.getLabel() : "");
    }
}
