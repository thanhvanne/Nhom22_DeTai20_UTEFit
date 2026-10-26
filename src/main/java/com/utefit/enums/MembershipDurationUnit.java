package com.utefit.enums;

import java.time.LocalDate;

public enum MembershipDurationUnit {
    DAY("ngày", 36600), WEEK("tuần", 5200), MONTH("tháng", 1200), YEAR("năm", 100);

    private final String label;
    private final int maximum;

    MembershipDurationUnit(String label, int maximum) {
        this.label = label;
        this.maximum = maximum;
    }

    public String getLabel() { return label; }
    public int getMaximum() { return maximum; }

    public LocalDate addTo(LocalDate start, int quantity) {
        return switch (this) {
            case DAY -> start.plusDays(quantity);
            case WEEK -> start.plusWeeks(quantity);
            case MONTH -> start.plusMonths(quantity);
            case YEAR -> start.plusYears(quantity);
        };
    }
}
