
package com.utefit.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Buoc Spring Security tao CSRF token truoc khi Thymeleaf render view.
 *
 * Spring Security 7 co the tai CSRF token theo co che deferred (lazy).
 * Neu trang co header lon duoc gui mot phan truoc khi xu ly form th:action,
 * viec tao HttpSession luc do se gay loi:
 * "Cannot create a session after the response has been committed".
 *
 * Advice chay trong giai doan khoi tao Model, truoc khi View duoc render.
 * Van giu nguyen CSRF cho moi POST va HttpSessionCsrfTokenRepository.
 */
@ControllerAdvice
public class CsrfTokenInitializationAdvice {

    @ModelAttribute
    public void initializeCsrfToken(HttpServletRequest request) {

        // GET thuong render giao dien HTML co form POST.
        // CsrfFilter da xu ly cac request POST truoc khi den Controller.
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return;
        }

        Object attribute = request.getAttribute(
                CsrfToken.class.getName()
        );

        if (attribute instanceof CsrfToken csrfToken) {

            // Buoc deferred token duoc tao truoc khi
            // response HTML co the bi flush/commit.
            csrfToken.getToken();
        }
    }
}
