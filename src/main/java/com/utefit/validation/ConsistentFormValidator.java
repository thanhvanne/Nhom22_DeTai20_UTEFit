package com.utefit.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ConsistentFormValidator implements ConstraintValidator<ConsistentForm,Object> {
    private static final Set<String> FIELDS = Set.of("password","newPassword","confirmPassword","openingTime","closingTime","startTime","endTime","scheduleDate","startDate","endDate");

    @Override
    public boolean isValid(Object form, ConstraintValidatorContext context) {
        if (form == null) return true;
        Map<String,String[]> values = new LinkedHashMap<>();
        var bean = new BeanWrapperImpl(form);
        for (String field : FIELDS) {
            if (bean.isReadableProperty(field) && bean.getPropertyValue(field) != null) values.put(field,new String[]{bean.getPropertyValue(field).toString()});
        }
        Map<String,String> errors = RequestChecks.errors("/validation","POST",values);
        if (errors.isEmpty()) return true;
        context.disableDefaultConstraintViolation();
        errors.forEach((field,message) -> context.buildConstraintViolationWithTemplate(message).addPropertyNode(field).addConstraintViolation());
        return false;
    }
}
