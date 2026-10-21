
package com.utefit.service;

import com.utefit.entity.Promotion;
import com.utefit.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminPromotionService {

    private final PromotionRepository repository;

    @Transactional(readOnly = true)
    public List<Promotion> findAll() {
        return repository.findAllByOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public Promotion findById(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new IllegalArgumentException(
                        "Không tìm thấy mã khuyến mãi."));
    }

    @Transactional
    public void save(Long id, Promotion form) {
        validate(id, form);

        Promotion promotion = id == null
                ? new Promotion()
                : findById(id);

        promotion.setCode(
                form.getCode().trim().toUpperCase(Locale.ROOT));
        promotion.setDiscountPercent(form.getDiscountPercent());
        promotion.setStartDate(form.getStartDate());
        promotion.setEndDate(form.getEndDate());
        promotion.setQuantity(form.getQuantity());
        promotion.setActive(form.getActive() == null
                || form.getActive());

        repository.save(promotion);
    }

    @Transactional
    public void toggle(Long id) {
        Promotion promotion = findById(id);
        promotion.setActive(!Boolean.TRUE.equals(promotion.getActive()));
        repository.save(promotion);
    }

    private void validate(Long id, Promotion form) {
        if (form.getCode() == null ||
                !form.getCode().trim().matches("[A-Za-z0-9_-]{3,50}")) {
            throw new IllegalArgumentException(
                    "Mã giảm giá phải có 3–50 ký tự: chữ, số, _ hoặc -.");
        }

        String code = form.getCode().trim();

        boolean duplicate = id == null
                ? repository.existsByCodeIgnoreCase(code)
                : repository.existsByCodeIgnoreCaseAndIdNot(code, id);

        if (duplicate) {
            throw new IllegalArgumentException(
                    "Mã khuyến mãi đã tồn tại.");
        }

        BigDecimal percent = form.getDiscountPercent();
        if (percent == null
                || percent.compareTo(BigDecimal.ZERO) <= 0
                || percent.compareTo(new BigDecimal("100")) > 0
                || percent.scale() > 2) {
            throw new IllegalArgumentException(
                    "Mức giảm phải lớn hơn 0, tối đa 100%, "
                            + "và không quá 2 chữ số thập phân.");
        }

        if (form.getStartDate() == null
                || form.getEndDate() == null
                || !form.getStartDate().isBefore(form.getEndDate())) {
            throw new IllegalArgumentException(
                    "Ngày bắt đầu phải trước ngày kết thúc.");
        }

        if (form.getQuantity() == null
                || form.getQuantity() < 0) {
            throw new IllegalArgumentException(
                    "Số lượng mã không được âm.");
        }
    }
}
