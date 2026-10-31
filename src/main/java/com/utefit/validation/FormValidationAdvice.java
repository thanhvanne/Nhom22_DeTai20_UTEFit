package com.utefit.validation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;
import java.beans.PropertyEditorSupport;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice(basePackages = "com.utefit.controller")
public class FormValidationAdvice {
    @InitBinder
    public void normalize(WebDataBinder binder) {
        binder.setAutoGrowCollectionLimit(100);
        for (String field : new String[]{"email","firstName","lastName","phone"}) {
            binder.registerCustomEditor(String.class,field,new PropertyEditorSupport() {
                @Override public void setAsText(String value) {
                    setValue(InputRules.normalize(value,InputRules.rule(field)));
                }
            });
        }
        binder.setDisallowedFields("class.*","createdAt","updatedAt","role","enabled","tokenVersion","passwordHash");
    }

    @ExceptionHandler(InputViolationException.class)
    public Object invalid(InputViolationException exception, HttpServletRequest request, HttpServletResponse response) {
        if (json(request)) {
            return ResponseEntity.badRequest().body(Map.of("error",exception.getMessage(),"message",exception.getMessage(),"fieldErrors",exception.getFieldErrors()));
        }
        String target = target(request);
        Map<String,Object> values = safeValues(request);
        var flash = RequestContextUtils.getOutputFlashMap(request);
        flash.put("validationErrors",exception.getFieldErrors());
        flash.put("validationValues",values);
        flash.put("validationAction",request.getRequestURI());
        RequestContextUtils.saveOutputFlashMap(request.getContextPath()+target,request,response);
        return new ModelAndView("redirect:"+target);
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public Object wrongType(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException exception,
                            HttpServletRequest request, HttpServletResponse response) {
        return invalid(new InputViolationException(Map.of(exception.getName(),
                "Giá trị sai định dạng hoặc vượt giới hạn. Vui lòng nhập lại.")),request,response);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public Object missingParameter(org.springframework.web.bind.MissingServletRequestParameterException exception,
                                   HttpServletRequest request, HttpServletResponse response) {
        return invalid(new InputViolationException(Map.of(exception.getParameterName(),
                "Vui lòng nhập hoặc chọn giá trị cho trường này.")),request,response);
    }

    public static Map<String,Object> safeValues(HttpServletRequest request) {
        Map<String,Object> values = new LinkedHashMap<>();
        request.getParameterMap().forEach((name,entries) -> {
            String lower = name.toLowerCase(java.util.Locale.ROOT);
            boolean otp = lower.equals("code") && !request.getRequestURI().startsWith("/admin/");
            if (!name.startsWith("_") && !lower.contains("password") && !lower.contains("token") && !otp
                    && entries.length > 0 && entries[0].length() <= 4000) values.put(name, entries.length == 1 ? entries[0] : entries);
        });
        return values;
    }

    private boolean json(HttpServletRequest request) {
        Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        return request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json")
                || "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))
                || handler instanceof HandlerMethod method && (method.hasMethodAnnotation(ResponseBody.class)
                || method.getBeanType().isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class));
    }

    private String target(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (variables instanceof Map<?,?> ids) {
            for (var entry : ids.entrySet()) {
                String field = entry.getKey().toString();
                String value = entry.getValue().toString();
                if ((field.equals("id") || field.endsWith("Id")) && InputRules.error("id",value) != null) {
                    int boundary = path.indexOf('/'+value);
                    if (boundary > 0) return path.substring(0,boundary);
                }
            }
        }
        if (path.equals("/resend-otp")) return "/verify-otp";
        if (path.startsWith("/member/profile")) return "/member/profile";
        if (path.startsWith("/member/memberships")) return "/member/memberships";
        if (path.startsWith("/member/trainer-requests")) return safeQuery(request,"trainerId").isEmpty() ? "/trainers" : "/member/trainer-requests/new"+safeQuery(request,"trainerId");
        if (path.equals("/pt/personal-schedules/create")) return safeQuery(request,"memberId").isEmpty() ? "/pt/trainer-requests" : path+safeQuery(request,"memberId");
        if (path.startsWith("/pt/profile")) return "/pt/profile/edit";
        if (path.startsWith("/admin/promotions/programs/") && path.endsWith("/state")) return "/admin/promotions";
        if (path.equals("/admin/promotions/save")) return "/admin/promotions"+safeQuery(request,"id").replace("?id=","?editId=");
        if (path.startsWith("/admin/managers")) return "/admin/managers";
        if (path.startsWith("/admin/checkins")) return "/admin/checkins";
        if (path.startsWith("/manager/checkins")) return "/manager"+safeQuery(request,"branchId");
        if (path.startsWith("/manager/schedules")) return safeQuery(request,"branchId").isEmpty() ? "/manager" : path+safeQuery(request,"branchId");
        if (path.matches("/admin/payments/sepay-reconciliation/[0-9]+/(verify|refund|activate|confirm-received)")) {
            return path.substring(0,path.lastIndexOf('/'))+(path.endsWith("/verify") || path.endsWith("/refund") ? "/refund-details" : "");
        }
        if (path.equals("/admin/payments/sepay-reconciliation/lookup")) return "/admin/payments/sepay-reconciliation";
        if (path.matches("/admin/payments/[0-9]+/.*")) return path.substring(0,path.lastIndexOf('/'));
        if (path.matches("/admin/trainers/[0-9]+/.*")) return "/admin/trainers";
        if (path.matches("/member/reviews/[0-9]+/edit")) return path;
        if (path.matches("/(admin|manager|member|pt)/.*/[0-9]+/(book|cancel|accept|reject|attendance|toggle|complete|remove|start)")) return "/"+path.split("/")[1]+"/dashboard";
        if (request.getMethod().equals("GET") && (request.getParameter("memberId") != null || request.getParameter("trainerId") != null)) return "/trainers";
        return path;
    }

    private String safeQuery(HttpServletRequest request, String field) {
        String value = request.getParameter(field);
        return value != null && !value.isBlank() && InputRules.error("id",value) == null ? "?"+field+"="+value : "";
    }
}
