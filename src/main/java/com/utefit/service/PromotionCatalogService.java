package com.utefit.service;

import com.utefit.config.LoyaltyProperties;
import com.utefit.repository.MembershipRepository;
import com.utefit.enums.AutomaticPromotionKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromotionCatalogService {
    private final LoyaltyProperties policy;
    private final MemberLoyaltyService loyalty;
    private final MembershipRepository memberships;
    private final AutomaticPromotionService automatic;
    public record Program(String kind, String name, String benefit, String condition, String status, boolean eligible) {}
    public boolean enabled() { return automatic.settings().anyActive(); }

    public List<Program> programs(String memberEmail) {
        var settings = automatic.settings();
        var summary = memberEmail == null ? null : loyalty.history(loyalty.member(memberEmail), settings).summary();
        List<Program> programs = new ArrayList<>();
        for (var kind : AutomaticPromotionKind.values()) {
            var rule = settings.rule(kind);
            String name = switch (kind) {
                case WELCOME -> "Khởi đầu cùng UTEFit"; case LOYAL -> "Gắn bó, tiến xa hơn"; case VALUE -> "Gói tập, giá trị lớn";
                case SILVER -> "Đặc quyền Silver"; case GOLD -> "Đặc quyền Gold";
            };
            String condition = switch (kind) {
                case WELCOME -> "Đơn đầu tiên · tối đa " + money(rule.discountCap()) + " · một lần/tài khoản. Đơn đang chờ giữ quyền; từng mua thật thì không nhận lại sau hoàn tiền.";
                case LOYAL -> "Từ " + rule.minimumMonths() + " tháng sử dụng thực tế không trùng ngày. Gia hạn cùng gói/chi nhánh trong " + rule.renewalGraceDays() + " ngày sau hết hạn hoặc khi gói đang dùng.";
                case VALUE -> "Giá gốc từ " + money(rule.valueThreshold()) + ": giảm " + money(rule.valueDiscount()) + ". Từ " + money(rule.premiumThreshold()) + ": giảm " + money(rule.premiumDiscount()) + ". Chỉ chọn một mức.";
                case SILVER -> "Chi tiêu hợp lệ từ " + money(policy.getSilverThreshold()) + " đến dưới " + money(policy.getGoldThreshold()) + ". Hạng mới dành cho đơn tiếp theo.";
                case GOLD -> "Chi tiêu hợp lệ từ " + money(policy.getGoldThreshold()) + ". Không cộng dồn với mã hoặc ưu đãi khác.";
            };
            if (kind != AutomaticPromotionKind.WELCOME && rule.discountCap() != null) condition += " Giảm tối đa " + money(rule.discountCap()) + ".";
            if (rule.startDate() != null) condition += " Từ ngày " + rule.startDate() + ".";
            if (rule.endDate() != null) condition += " Đến ngày " + rule.endDate() + ".";
            boolean eligible = summary != null && rule.available() && switch (kind) {
                case WELCOME -> summary.welcomeAvailable(); case LOYAL -> summary.loyal();
                case VALUE -> false; case SILVER, GOLD -> summary.tier().equals(kind.name());
            };
            String status = !rule.available() ? rule.state() : summary == null ? "Đăng nhập để kiểm tra" : switch (kind) {
                case WELCOME -> !summary.newCustomer() ? "Đã có lịch sử mua hợp lệ" : summary.welcomeAvailable() ? "Đủ điều kiện" : "Có đơn đang chờ";
                case LOYAL -> summary.loyal() ? "Đủ thời gian · kiểm tra gói gia hạn khi mua" : "Chưa đủ thời gian sử dụng hợp lệ";
                case VALUE -> "Chọn gói để kiểm tra giá gốc";
                case SILVER, GOLD -> eligible ? "Đủ điều kiện" : "Hạng hiện tại: " + summary.tier();
            };
            programs.add(new Program(kind.name(), name, kind == AutomaticPromotionKind.VALUE ? money(rule.premiumDiscount()) : percent(rule.percent()), condition, status, eligible));
        }
        return List.copyOf(programs);
    }

    public long uses(String email, String code) { return memberships.promotionUses(loyalty.member(email).getId(), code); }
    public static String money(BigDecimal amount) { return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(amount) + "đ"; }
    private String percent(BigDecimal amount) { return amount.stripTrailingZeros().toPlainString() + "%"; }
}
