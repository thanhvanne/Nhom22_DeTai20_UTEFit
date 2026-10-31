package com.utefit.validation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class FormValidationInterceptor implements HandlerInterceptor {
    private final ImageValidator images;
    private final VideoValidator videos;

    public FormValidationInterceptor(ImageValidator images, VideoValidator videos) {
        this.images = images;
        this.videos = videos;
    }

    private boolean applies(HttpServletRequest request, Object handler) {
        return handler instanceof HandlerMethod method
                && method.getBeanType().getPackageName().startsWith("com.utefit.controller")
                && !request.getRequestURI().startsWith("/api/")
                && !request.getRequestURI().startsWith("/payments/vnpay/")
                && !method.getBeanType().getSimpleName().contains("Webhook");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!applies(request,handler)) return true;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        Map<String,String> errors = new LinkedHashMap<>(RequestChecks.errors(path,request.getMethod(),request.getParameterMap()));
        if (path.matches("/admin/promotions/programs/[A-Z_]+") && request.getMethod().equals("POST")) {
            errors.clear();
        }
        Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (variables instanceof Map<?,?> ids) {
            ids.forEach((name,value) -> {
                String field = name.toString();
                if (field.equals("id") || field.endsWith("Id")) {
                    String error = InputRules.error("id", value.toString());
                    if (error != null) errors.put(field,error);
                }
            });
        }
        HandlerMethod method = (HandlerMethod) handler;
        for (var parameter : method.getMethodParameters()) {
            RequestParam annotation = parameter.getParameterAnnotation(RequestParam.class);
            if (annotation == null) continue;
            String name = !annotation.name().isEmpty() ? annotation.name() : !annotation.value().isEmpty() ? annotation.value() : parameter.getParameterName();
            String value = request.getParameter(name);
            if (annotation.required() && annotation.defaultValue().equals(org.springframework.web.bind.annotation.ValueConstants.DEFAULT_NONE)
                    && (value == null || value.isBlank()) && !org.springframework.web.multipart.MultipartFile.class.isAssignableFrom(parameter.getParameterType())) {
                errors.putIfAbsent(name,"Vui lòng nhập hoặc chọn giá trị cho trường này.");
            }
            if (value != null && !value.isBlank() && parameter.getParameterType().isEnum()) {
                boolean found = java.util.Arrays.stream(parameter.getParameterType().getEnumConstants()).anyMatch(option -> option.toString().equals(value));
                if (!found) errors.put(name,"Lựa chọn không hợp lệ. Vui lòng chọn lại.");
            }
        }
        if (request instanceof MultipartHttpServletRequest multipart) {
            if (path.equals("/member/profile/avatar") && (multipart.getFile("avatar") == null || multipart.getFile("avatar").isEmpty())) errors.put("avatar","Vui lòng chọn ảnh đại diện.");
            multipart.getMultiFileMap().forEach((name,files) -> files.forEach(file -> {
                if (file.isEmpty()) return;
                try {
                    if (name.equals("video")) videos.validate(file);
                    else images.validate(file);
                } catch (IllegalArgumentException exception) {
                    errors.putIfAbsent(name,exception.getMessage());
                }
            }));
        }
        if (!errors.isEmpty()) throw new InputViolationException(errors);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView view) {
        if (view == null || !applies(request,handler) || view.getViewName() == null) return;
        Map<String,String> errors = new LinkedHashMap<>();
        java.util.Set<String> available = new java.util.LinkedHashSet<>(request.getParameterMap().keySet());
        if (request instanceof MultipartHttpServletRequest multipart) available.addAll(multipart.getFileMap().keySet());
        view.getModel().values().forEach(value -> {
            if (value instanceof BindingResult binding) {
                if (binding.getTarget() != null) {
                    for (var property : new org.springframework.beans.BeanWrapperImpl(binding.getTarget()).getPropertyDescriptors()) available.add(property.getName());
                }
                binding.getFieldErrors().forEach(error -> errors.putIfAbsent(error.getField(), error.isBindingFailure()
                        ? "Giá trị sai định dạng hoặc vượt giới hạn. Vui lòng nhập lại." : error.getDefaultMessage()));
                binding.getGlobalErrors().forEach(error -> errors.putAll(ValidationMessages.fields(error.getDefaultMessage(),available)));
            }
        });
        Map<String,Object> flash = org.springframework.web.servlet.support.RequestContextUtils.getOutputFlashMap(request);
        for (String name : new String[]{"error","errorMessage"}) {
            Object message = view.getModel().getOrDefault(name,flash.get(name));
            if (message instanceof String text) errors.putAll(ValidationMessages.fields(text,available));
        }
        if (!errors.isEmpty()) {
            if (view.getViewName().startsWith("redirect:")) {
                flash.put("validationErrors",errors);
                flash.put("validationValues",FormValidationAdvice.safeValues(request));
                flash.put("validationAction",request.getRequestURI());
            } else {
                view.addObject("validationErrors",errors);
                view.addObject("validationAction",request.getRequestURI());
            }
        }
    }
}
