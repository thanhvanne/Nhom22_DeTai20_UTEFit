package com.utefit.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidInputValidator implements ConstraintValidator<ValidInput, Object> {
    private String field;

    @Override
    public void initialize(ValidInput annotation) {
        field = annotation.value();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        String message = InputRules.error(field, value == null ? null : value.toString());
        if (message == null) return true;
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
