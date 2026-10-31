package com.utefit.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.beans.BeanWrapperImpl;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FormChecks {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private FormChecks() {}

    public static void validate(Object form) {
        var bean = new BeanWrapperImpl(form);
        String path = switch (form.getClass().getSimpleName()) {
            case "BranchFormDto" -> "/admin/branches/create";
            case "ClassCategoryFormDto" -> "/admin/categories/create";
            case "MembershipPlanFormDto" -> "/admin/membership-plans/create";
            case "Promotion" -> "/admin/promotions/save";
            default -> "/validation";
        };
        Map<String,String[]> values = new LinkedHashMap<>();
        for (var property : bean.getPropertyDescriptors()) {
            String name = property.getName();
            if (property.getPropertyType() == String.class && bean.isWritableProperty(name)) {
                bean.setPropertyValue(name, InputRules.normalize((String) bean.getPropertyValue(name), InputRules.rule(path,"POST",name)));
            }
            if (!name.equals("class") && bean.isReadableProperty(name) && bean.getPropertyValue(name) != null
                    && !(bean.getPropertyValue(name) instanceof org.springframework.web.multipart.MultipartFile)) {
                Object value = bean.getPropertyValue(name);
                values.put(name,value instanceof java.util.Collection<?> collection
                        ? collection.stream().map(Object::toString).toArray(String[]::new) : new String[]{value.toString()});
            }
        }
        Map<String, String> errors = new LinkedHashMap<>(RequestChecks.errors(path,"POST",values));
        VALIDATOR.validate(form).forEach(violation -> errors.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage()));
        String passwordField = bean.isReadableProperty("newPassword") ? "newPassword" : "password";
        if (bean.isReadableProperty("confirmPassword") && bean.isReadableProperty(passwordField)) {
            Object password = bean.getPropertyValue(passwordField);
            if (password != null && !password.equals(bean.getPropertyValue("confirmPassword"))) {
                errors.put("confirmPassword", "Mật khẩu xác nhận không khớp.");
            }
        }
        if (!errors.isEmpty()) throw new InputViolationException(errors);
    }
}
