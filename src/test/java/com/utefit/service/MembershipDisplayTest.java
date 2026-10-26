package com.utefit.service;

import com.utefit.enums.MembershipBenefit;
import com.utefit.util.MembershipDisplay;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class MembershipDisplayTest {
    private final MembershipDisplay display = new MembershipDisplay();

    @Test void vietnameseMoneyDoesNotDependOnDefaultLocale() {
        assertEquals("20.000đ", display.money(new BigDecimal("20000.00")));
        assertEquals("300.000đ", display.money(new BigDecimal("300000.00")));
        assertEquals("2.800.000đ", display.money(new BigDecimal("2800000.00")));
    }

    @Test void legacyNamesAreTranslatedWithoutChangingCustomNames() {
        for (int branch = 1; branch <= 5; branch++) {
            assertEquals("UTEFit - Chi nhánh " + branch, display.name("UTEFit Demo Branch " + branch));
            assertEquals("Gói tập 1 tháng", display.name("DEMO B" + branch + " - 1 month"));
            assertEquals("Gói tập 3 tháng - Tặng thêm 2 tuần", display.name("DEMO B" + branch + " - 3 months + 2 weeks bonus"));
        }
        assertEquals("Gói riêng của nhóm", display.name("Gói riêng của nhóm"));
        assertEquals("DEMO B1 - custom", display.name("DEMO B1 - custom"));
    }

    @Test void benefitsAreLabelsOnlyAndDoNotChangeEnumCodes() {
        assertEquals("Phòng gym", display.benefit(MembershipBenefit.GYM));
        assertEquals("Yoga", display.benefit(MembershipBenefit.YOGA));
        assertEquals("Huấn luyện viên cá nhân", display.benefit("PT"));
        assertEquals("Tất cả dịch vụ", display.benefit(MembershipBenefit.ALL));
        assertEquals("GYM", MembershipBenefit.GYM.name());
    }
}
