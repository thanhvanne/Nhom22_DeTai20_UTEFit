package com.utefit.controller.admin;

import com.utefit.service.SepayWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class SepayReconciliationController {
    private final SepayWebhookService webhooks;
    private final com.utefit.service.SepayReconciliationLookup lookup;
    private final com.utefit.service.SepayManualReconciliationService manual;

    @GetMapping("/admin/payments/sepay-reconciliation/{id}")
    public String manualCase(@PathVariable Long id, Model model,
                             @RequestParam(defaultValue = "false") boolean notReceived,
                             jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        model.addAttribute("caseView", manual.view(id));
        model.addAttribute("activeMenu", "payments");
        model.addAttribute("notReceived", notReceived);
        return "admin/payments/sepay-manual";
    }

    @GetMapping("/admin/payments/sepay-reconciliation/{id}/refund-details")
    public String refundDetails(@PathVariable Long id, Model model,
                                jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        model.addAttribute("caseView", manual.view(id));
        model.addAttribute("activeMenu", "payments");
        return "admin/payments/sepay-manual-refund";
    }

    @PostMapping("/admin/payments/sepay-reconciliation/{id}/confirm-received")
    public String confirmReceived(@PathVariable Long id, @RequestParam String bankReference,
                                  @RequestParam(defaultValue = "false") boolean independentlyVerified,
                                  org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        return perform(id, flash, () -> manual.confirmReceived(id, bankReference, independentlyVerified),
                "Đã ghi nhận thanh toán và kích hoạt Membership theo snapshot.");
    }

    @PostMapping("/admin/payments/sepay-reconciliation/{id}/verify")
    public String verify(@PathVariable Long id,
                         @ModelAttribute com.utefit.service.SepayManualReconciliationService.ReceiptEvidence evidence,
                         org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        return perform(id, flash, () -> manual.verifyReceipt(id, evidence), "Đã lưu xác minh tiền nhận. Chưa kích hoạt gói.");
    }

    @PostMapping("/admin/payments/sepay-reconciliation/{id}/activate")
    public String activate(@PathVariable Long id, @RequestParam Long recordId,
                           org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        return perform(id, flash, () -> manual.activate(id, recordId), "Đã ghi PAID và kích hoạt Membership theo snapshot.");
    }

    @PostMapping("/admin/payments/sepay-reconciliation/{id}/refund")
    public String refund(@PathVariable Long id, @RequestParam Long recordId,
                         @ModelAttribute com.utefit.service.SepayManualReconciliationService.RefundEvidence evidence,
                         org.springframework.web.servlet.mvc.support.RedirectAttributes flash) {
        return perform(id, flash, () -> manual.confirmRefund(id, recordId, evidence), "Đã ghi nhận hoàn đủ tiền thực tế và thu hồi kỳ Membership; đã kiểm tra các lớp tương lai. Không tạo lệnh chuyển tiền.");
    }

    private String perform(Long id, org.springframework.web.servlet.mvc.support.RedirectAttributes flash,
                           Runnable action, String success) {
        try { action.run(); flash.addFlashAttribute("manualSuccess", success); }
        catch (IllegalStateException ex) { flash.addFlashAttribute("manualError", ex.getMessage()); }
        catch (org.springframework.dao.DataAccessException | org.springframework.transaction.TransactionException ex) {
            flash.addFlashAttribute("manualError", "Xung đột giao dịch hoặc lỗi lưu dữ liệu. Thao tác đã rollback; tải lại và kiểm tra lịch sử trước khi thử lại.");
        }
        return "redirect:/admin/payments/sepay-reconciliation/" + id;
    }

    @PostMapping("/admin/payments/sepay-reconciliation/lookup")
    public String lookup(@RequestParam java.time.LocalDate date, @RequestParam(defaultValue = "1") int apiPage,
                         @RequestParam(defaultValue = "0") int page, Model model,
                         jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        try { model.addAttribute("apiResult", lookup.lookup(date, apiPage)); }
        catch (IllegalStateException ex) { model.addAttribute("apiError", ex.getMessage()); }
        return index(page, model);
    }

    @GetMapping("/admin/payments/sepay-reconciliation")
    public String index(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("transactions", webhooks.reconciliation(page));
        model.addAttribute("pendingOrders", manual.pending(page));
        model.addAttribute("activeMenu", "payments");
        return "admin/payments/sepay-reconciliation";
    }
}
