package com.utefit.service;

import com.utefit.config.LoyaltyProperties;
import com.utefit.dto.admin.AutomaticPromotionForm;
import com.utefit.entity.AutomaticPromotion;
import com.utefit.enums.AutomaticPromotionKind;
import com.utefit.repository.*;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AutomaticPromotionService {
    private final AutomaticPromotionRepository programs;
    private final MembershipRepository memberships;
    private final LoyaltyProperties defaults;
    private final Clock clock;
    @PersistenceContext private EntityManager entityManager;

    public record Rule(AutomaticPromotionKind kind, boolean active, boolean available, BigDecimal percent,
                       BigDecimal discountCap, Integer minimumMonths, Integer renewalGraceDays,
                       BigDecimal valueThreshold, BigDecimal valueDiscount, BigDecimal premiumThreshold, BigDecimal premiumDiscount,
                       LocalDate startDate, LocalDate endDate, Long version, boolean persisted,
                       LocalDateTime updatedAt, String updatedBy) {
        public String state() {
            if (!persisted) return "Chưa khởi tạo DB";
            if (!active) return "Tạm dừng";
            return available ? "Đang hoạt động" : "Ngoài thời gian áp dụng";
        }
    }
    public record Settings(Map<AutomaticPromotionKind, Rule> rules) {
        public Settings { rules = Map.copyOf(rules); }
        public Rule rule(AutomaticPromotionKind kind) { return rules.get(kind); }
        public boolean anyActive() { return rules.values().stream().anyMatch(Rule::available); }
    }
    public record Usage(long paid, long pending) {}

    public Settings settings() {
        var stored = programs.findAll();
        var rules = new EnumMap<AutomaticPromotionKind, Rule>(AutomaticPromotionKind.class);
        for (var kind : AutomaticPromotionKind.values()) rules.put(kind, fallback(kind, stored.isEmpty() && defaults.isEnabled()));
        for (var program : stored) rules.put(program.getKind(), rule(program));
        return new Settings(rules);
    }

    private Rule fallback(AutomaticPromotionKind kind, boolean active) {
        BigDecimal percent = switch (kind) {
            case WELCOME -> defaults.getWelcomePercent(); case LOYAL -> defaults.getRenewalPercent();
            case SILVER -> defaults.getSilverPercent(); case GOLD -> defaults.getGoldPercent(); case VALUE -> null;
        };
        return new Rule(kind, active, active, percent, kind == AutomaticPromotionKind.WELCOME ? defaults.getWelcomeCap() : null,
                kind == AutomaticPromotionKind.LOYAL ? defaults.getLoyaltyMonths() : null,
                kind == AutomaticPromotionKind.LOYAL ? defaults.getRenewalGraceDays() : null,
                kind == AutomaticPromotionKind.VALUE ? defaults.getValueThreshold() : null,
                kind == AutomaticPromotionKind.VALUE ? defaults.getValueDiscount() : null,
                kind == AutomaticPromotionKind.VALUE ? defaults.getPremiumThreshold() : null,
                kind == AutomaticPromotionKind.VALUE ? defaults.getPremiumDiscount() : null,
                null, null, null, false, null, null);
    }

    private Rule rule(AutomaticPromotion program) {
        LocalDate today = LocalDate.now(clock.withZone(SepayPaymentService.ZONE));
        boolean available = program.isActive() && (program.getStartDate() == null || !program.getStartDate().isAfter(today))
                && (program.getEndDate() == null || !program.getEndDate().isBefore(today));
        return new Rule(program.getKind(), program.isActive(), available, program.getPercent(), program.getDiscountCap(),
                program.getMinimumMonths(), program.getRenewalGraceDays(), program.getValueThreshold(), program.getValueDiscount(),
                program.getPremiumThreshold(), program.getPremiumDiscount(), program.getStartDate(), program.getEndDate(),
                program.getVersion(), true, program.getUpdatedAt(), program.getUpdatedBy());
    }

    public AutomaticPromotionForm form(AutomaticPromotionKind kind) {
        return form(settings().rule(kind));
    }

    private AutomaticPromotionForm form(Rule rule) {
        var form = new AutomaticPromotionForm();
        form.setVersion(rule.version()); form.setActive(rule.active()); form.setPercent(rule.percent()); form.setDiscountCap(rule.discountCap());
        form.setMinimumMonths(rule.minimumMonths()); form.setRenewalGraceDays(rule.renewalGraceDays());
        form.setValueThreshold(rule.valueThreshold()); form.setValueDiscount(rule.valueDiscount());
        form.setPremiumThreshold(rule.premiumThreshold()); form.setPremiumDiscount(rule.premiumDiscount());
        form.setStartDate(rule.startDate()); form.setEndDate(rule.endDate());
        return form;
    }

    public Map<String, String> errors(AutomaticPromotionKind kind, AutomaticPromotionForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (form.getVersion() == null || form.getVersion() < 0) errors.put("version", "Thiếu phiên bản cấu hình. Tải lại trang sau khi chạy V717.");
        if (form.getActive() == null) errors.put("active", "Vui lòng chọn trạng thái.");
        if (kind != AutomaticPromotionKind.VALUE) {
            var percent = form.getPercent();
            if (percent == null || percent.signum() <= 0 || percent.compareTo(new BigDecimal("100")) >= 0 || percent.scale() > 2)
                errors.put("percent", "Phần trăm phải lớn hơn 0, nhỏ hơn 100, tối đa 2 chữ số thập phân.");
        } else if (form.getPercent() != null) errors.put("percent", "VALUE sử dụng số tiền cố định, không dùng phần trăm.");
        if (kind == AutomaticPromotionKind.WELCOME || kind == AutomaticPromotionKind.SILVER || kind == AutomaticPromotionKind.GOLD) {
            money(errors, "discountCap", form.getDiscountCap(), kind == AutomaticPromotionKind.WELCOME);
        } else if (form.getDiscountCap() != null) errors.put("discountCap", "Chương trình này không hỗ trợ trần giảm.");
        if (kind == AutomaticPromotionKind.LOYAL) {
            if (form.getMinimumMonths() == null || form.getMinimumMonths() < 1 || form.getMinimumMonths() > 1200) errors.put("minimumMonths", "Số tháng phải từ 1 đến 1200.");
            if (form.getRenewalGraceDays() == null || form.getRenewalGraceDays() < 0 || form.getRenewalGraceDays() > 3650) errors.put("renewalGraceDays", "Số ngày gia hạn phải từ 0 đến 3650.");
        } else if (form.getMinimumMonths() != null || form.getRenewalGraceDays() != null) errors.put("minimumMonths", "Chỉ LOYAL có điều kiện thời gian gắn bó/gia hạn.");
        if (kind == AutomaticPromotionKind.VALUE) {
            money(errors, "valueThreshold", form.getValueThreshold(), true); money(errors, "valueDiscount", form.getValueDiscount(), true);
            money(errors, "premiumThreshold", form.getPremiumThreshold(), true); money(errors, "premiumDiscount", form.getPremiumDiscount(), true);
            if (errors.isEmpty()) {
                if (form.getPremiumThreshold().compareTo(form.getValueThreshold()) <= 0) errors.put("premiumThreshold", "Ngưỡng 2 phải lớn hơn ngưỡng 1.");
                if (form.getValueDiscount().compareTo(form.getValueThreshold()) >= 0) errors.put("valueDiscount", "Giảm mức 1 phải nhỏ hơn ngưỡng 1.");
                if (form.getPremiumDiscount().compareTo(form.getPremiumThreshold()) >= 0) errors.put("premiumDiscount", "Giảm mức 2 phải nhỏ hơn ngưỡng 2.");
                if (form.getPremiumDiscount().compareTo(form.getValueDiscount()) < 0) errors.put("premiumDiscount", "Giảm mức 2 không được nhỏ hơn mức 1.");
            }
        } else if (form.getValueThreshold() != null || form.getValueDiscount() != null || form.getPremiumThreshold() != null || form.getPremiumDiscount() != null)
            errors.put("valueThreshold", "Chỉ VALUE hỗ trợ hai ngưỡng giá trị đơn.");
        if (form.getStartDate() != null && (form.getStartDate().getYear() < 1 || form.getStartDate().getYear() > 9999)) errors.put("startDate", "Ngày không hợp lệ.");
        if (form.getEndDate() != null && (form.getEndDate().getYear() < 1 || form.getEndDate().getYear() > 9999)) errors.put("endDate", "Ngày không hợp lệ.");
        if (form.getStartDate() != null && form.getEndDate() != null && form.getEndDate().isBefore(form.getStartDate())) errors.put("endDate", "Ngày kết thúc không được trước ngày bắt đầu.");
        return errors;
    }

    private void money(Map<String, String> errors, String field, BigDecimal amount, boolean required) {
        if (amount == null && !required) return;
        if (amount == null || amount.signum() <= 0 || amount.stripTrailingZeros().scale() > 0 || amount.compareTo(new BigDecimal("9999999999999")) > 0)
            errors.put(field, "Số tiền phải là VND nguyên dương, tối đa 13 chữ số.");
    }

    @Transactional
    public void save(AutomaticPromotionKind kind, AutomaticPromotionForm form) {
        com.utefit.validation.FormChecks.validate(form);
        String actor = requireAdmin();
        var errors = errors(kind, form);
        if (!errors.isEmpty()) throw new IllegalArgumentException(errors.values().iterator().next());
        var program = locked(kind, form.getVersion());
        program.setActive(form.getActive()); program.setPercent(form.getPercent()); program.setDiscountCap(form.getDiscountCap());
        program.setMinimumMonths(form.getMinimumMonths()); program.setRenewalGraceDays(form.getRenewalGraceDays());
        program.setValueThreshold(form.getValueThreshold()); program.setValueDiscount(form.getValueDiscount());
        program.setPremiumThreshold(form.getPremiumThreshold()); program.setPremiumDiscount(form.getPremiumDiscount());
        program.setStartDate(form.getStartDate()); program.setEndDate(form.getEndDate());
        program.setUpdatedAt(LocalDateTime.now(clock.withZone(SepayPaymentService.ZONE))); program.setUpdatedBy(actor);
        programs.saveAndFlush(program);
    }

    @Transactional
    public void setActive(AutomaticPromotionKind kind, long version, boolean active) {
        String actor = requireAdmin();
        var program = locked(kind, version);
        var form = form(rule(program)); form.setActive(active);
        var errors = errors(kind, form);
        if (!errors.isEmpty()) throw new IllegalArgumentException(errors.values().iterator().next());
        program.setActive(active); program.setUpdatedAt(LocalDateTime.now(clock.withZone(SepayPaymentService.ZONE))); program.setUpdatedBy(actor);
        programs.saveAndFlush(program);
    }

    private AutomaticPromotion locked(AutomaticPromotionKind kind, long version) {
        var program = programs.lockByKind(kind).orElseThrow(() -> new IllegalStateException("Chưa khởi tạo chương trình trong DB. Cần duyệt/chạy V717 trước."));
        entityManager.refresh(program);
        if (!Objects.equals(program.getVersion(), version)) throw new IllegalStateException("Cấu hình đã được người khác thay đổi. Tải lại trang trước khi lưu; không ghi đè dữ liệu mới.");
        return program;
    }

    private String requireAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getAuthorities().stream().noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")))
            throw new AccessDeniedException("Chỉ Admin được quản lý chương trình tự động.");
        return authentication.getName();
    }

    public Usage usage(AutomaticPromotionKind kind) {
        boolean tier = kind == AutomaticPromotionKind.SILVER || kind == AutomaticPromotionKind.GOLD;
        return new Usage(memberships.automaticPaidUses(tier ? "TIER" : kind.name(), tier ? "Đặc quyền " + kind.name() : null),
                memberships.automaticPendingUses(tier ? "TIER" : kind.name(), tier ? "Đặc quyền " + kind.name() : null));
    }
}
