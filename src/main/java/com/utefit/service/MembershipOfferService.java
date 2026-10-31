package com.utefit.service;

import com.utefit.dto.member.MembershipPurchaseRequest;
import com.utefit.entity.*;
import com.utefit.enums.MembershipStatus;
import com.utefit.enums.AutomaticPromotionKind;
import com.utefit.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.*;
import java.math.*;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MembershipOfferService {
    private final AutomaticPromotionService automatic;
    private final MemberLoyaltyService loyalty;
    private final PromotionRepository promotions;
    private final MembershipRepository memberships;
    private final Clock clock;
    @PersistenceContext private EntityManager entityManager;

    public record Offer(String kind, String name, BigDecimal discount, BigDecimal percent) {}
    public record Quote(BigDecimal originalPrice, String kind, String name, BigDecimal discount,
                        BigDecimal finalPrice, BigDecimal percent, String promotionCode, List<Offer> alternatives) {}

    @Transactional(readOnly = true)
    public Quote preview(User member, MembershipPlan plan, Long branchId, MembershipPurchaseRequest request) {
        return calculate(member, plan, branchId, request, false, automatic.settings());
    }

    public Quote snapshot(Membership membership) {
        if (membership.getQuotedPrice() == null || membership.getQuotedPrice().signum() <= 0) throw new IllegalArgumentException("Đơn cũ thiếu snapshot giá; cần quản trị viên đối soát.");
        BigDecimal original = membership.getOriginalPrice() == null ? membership.getQuotedPrice() : membership.getOriginalPrice();
        String label = membership.getOfferLabel() == null ? membership.getPromotionCode() == null ? "Giá đã chốt" : "Mã " + membership.getPromotionCode() : membership.getOfferLabel();
        return new Quote(original, membership.getOfferKind() == null ? "LEGACY" : membership.getOfferKind(), label + " · Đơn chờ #" + membership.getId(),
                original.subtract(membership.getQuotedPrice()), membership.getQuotedPrice(), membership.getDiscountPercent() == null ? BigDecimal.ZERO : membership.getDiscountPercent(), membership.getPromotionCode(), List.of());
    }

    @Transactional
    public void apply(User member, Membership membership, MembershipPurchaseRequest request) {
        if (membership.getId() != null || membership.getStatus() != MembershipStatus.PENDING || !membership.getUser().getId().equals(member.getId())) throw new IllegalStateException("Chỉ chốt giá một lần khi tạo đơn mới chưa lưu.");
        if (memberships.findByUser_IdOrderByCreatedAtDesc(member.getId()).stream()
                .anyMatch(order -> "WELCOME".equals(order.getOfferKind()) && order.getStatus() == MembershipStatus.PENDING)) throw new IllegalArgumentException("Đơn ưu đãi lần đầu đang chờ. Hoàn tất hoặc hủy đúng quy trình trước khi tạo đơn khác.");
        Quote quote = calculate(member, membership.getPlan(), membership.getBranch().getId(), request, true, automatic.settings());
        membership.setOriginalPrice(quote.originalPrice());
        membership.setQuotedPrice(quote.finalPrice());
        membership.setDiscountPercent(quote.percent());
        membership.setDiscountAmount(quote.discount());
        membership.setOfferKind(quote.kind());
        membership.setOfferLabel(quote.name());
        membership.setPromotionCode(quote.promotionCode());
        membership.setPricingSelection(selection(request));
    }

    private Quote calculate(User member, MembershipPlan plan, Long branchId, MembershipPurchaseRequest request, boolean reserve, AutomaticPromotionService.Settings settings) {
        String selection = selection(request);
        BigDecimal price = plan.getPrice();
        if (price == null || price.signum() <= 0 || price.stripTrailingZeros().scale() > 0) throw new IllegalArgumentException("Giá gốc phải là số VND nguyên dương.");
        String code = request.getPromotionCode();
        List<Offer> offers = new ArrayList<>();
        offers.add(new Offer("NONE", "Không áp dụng ưu đãi", BigDecimal.ZERO, BigDecimal.ZERO));
        if (code != null && !code.isBlank()) {
            if (code.trim().length() > 50) throw new IllegalArgumentException("Mã khuyến mãi không hợp lệ.");
            Promotion promotion = reserve ? promotions.lockByCode(code.trim()).orElseThrow(() -> new IllegalArgumentException("Mã khuyến mãi không tồn tại."))
                    : promotions.findByCodeIgnoreCase(code.trim()).orElseThrow(() -> new IllegalArgumentException("Mã khuyến mãi không tồn tại."));
            if (reserve) entityManager.refresh(promotion);
            validateCode(member, promotion, price);
            BigDecimal discount = percentage(price, promotion.getDiscountPercent());
            if (promotion.getMaxDiscountAmount() != null) discount = discount.min(promotion.getMaxDiscountAmount());
            Quote quote = quote(price, new Offer("CODE", "Mã " + promotion.getCode(), discount, promotion.getDiscountPercent()), promotion.getCode().toUpperCase(Locale.ROOT), offers);
            if (reserve) promotion.setQuantity(promotion.getQuantity() - 1);
            return quote;
        }
        if (settings.anyActive() && selection.equals("AUTO")) {
            var history = loyalty.history(member, settings);
            var summary = history.summary();
            var welcome = settings.rule(AutomaticPromotionKind.WELCOME);
            var loyal = settings.rule(AutomaticPromotionKind.LOYAL);
            var value = settings.rule(AutomaticPromotionKind.VALUE);
            if (welcome.available() && summary.welcomeAvailable()) offers.add(new Offer("WELCOME", "Chào mừng hội viên mới", cappedPercentage(price, welcome), welcome.percent()));
            if (summary.percent().signum() > 0) offers.add(new Offer("TIER", "Đặc quyền " + summary.tier(), cappedPercentage(price, settings.rule(AutomaticPromotionKind.valueOf(summary.tier()))), summary.percent()));
            LocalDate today = LocalDate.now(clock.withZone(SepayPaymentService.ZONE));
            boolean renewal = history.paidMemberships().stream().anyMatch(existing -> existing.getPlan().getId().equals(plan.getId()) && existing.getBranch().getId().equals(branchId)
                    && existing.getStartDate() != null && !existing.getStartDate().isAfter(today) && existing.getEndDate() != null && !existing.getEndDate().isBefore(today.minusDays(loyal.renewalGraceDays())));
            if (loyal.available() && summary.loyal() && renewal) offers.add(new Offer("LOYAL", "Gia hạn gắn bó", percentage(price, loyal.percent()), loyal.percent()));
            if (value.available() && price.compareTo(value.premiumThreshold()) >= 0) offers.add(new Offer("VALUE", "Gói tập giá trị cao", value.premiumDiscount(), BigDecimal.ZERO));
            else if (value.available() && price.compareTo(value.valueThreshold()) >= 0) offers.add(new Offer("VALUE", "Gói tập giá trị cao", value.valueDiscount(), BigDecimal.ZERO));
        }
        Offer best = offers.stream().filter(offer -> offer.discount().compareTo(price) < 0).max(Comparator.comparing(Offer::discount)).orElse(offers.get(0));
        return quote(price, best, null, offers);
    }

    private Quote quote(BigDecimal price, Offer offer, String code, List<Offer> alternatives) {
        BigDecimal finalPrice = price.subtract(offer.discount()).setScale(0, RoundingMode.HALF_UP);
        if (finalPrice.signum() <= 0 || offer.discount().signum() < 0) throw new IllegalArgumentException("Ưu đãi miễn phí chưa hỗ trợ thanh toán; chọn ưu đãi khác.");
        return new Quote(price, offer.kind(), offer.name(), price.subtract(finalPrice), finalPrice, offer.percent(), code, List.copyOf(alternatives));
    }

    private BigDecimal percentage(BigDecimal price, BigDecimal percent) {
        if (percent == null || percent.signum() <= 0 || percent.compareTo(BigDecimal.valueOf(100)) >= 0) throw new IllegalArgumentException("Phần trăm khuyến mãi không hợp lệ.");
        return price.subtract(price.multiply(BigDecimal.valueOf(100).subtract(percent)).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP));
    }

    private BigDecimal cappedPercentage(BigDecimal price, AutomaticPromotionService.Rule rule) {
        BigDecimal discount = percentage(price, rule.percent());
        return rule.discountCap() == null ? discount : discount.min(rule.discountCap());
    }

    private void validateCode(User member, Promotion promotion, BigDecimal price) {
        LocalDate today = LocalDate.now(clock.withZone(SepayPaymentService.ZONE));
        if (!Boolean.TRUE.equals(promotion.getActive()) || promotion.getStartDate() == null || promotion.getEndDate() == null || promotion.getStartDate().isAfter(today)
                || promotion.getEndDate().isBefore(today) || promotion.getQuantity() == null || promotion.getQuantity() <= 0) throw new IllegalArgumentException("Mã khuyến mãi chưa có hiệu lực, đã hết hạn hoặc hết lượt.");
        if (promotion.getMinOrderAmount() != null && price.compareTo(promotion.getMinOrderAmount()) < 0) throw new IllegalArgumentException("Đơn chưa đạt giá gốc tối thiểu của mã.");
        if (promotion.getPerUserLimit() != null && memberships.promotionUses(member.getId(), promotion.getCode()) >= promotion.getPerUserLimit()) throw new IllegalArgumentException("Bạn đã dùng hoặc giữ đủ lượt của mã này.");
    }

    public String selection(MembershipPurchaseRequest request) {
        String selection = request.getOfferSelection() == null ? "AUTO" : request.getOfferSelection();
        if (!selection.equals("AUTO") && !selection.equals("NONE")) throw new IllegalArgumentException("Lựa chọn ưu đãi không hợp lệ.");
        return selection;
    }
}
