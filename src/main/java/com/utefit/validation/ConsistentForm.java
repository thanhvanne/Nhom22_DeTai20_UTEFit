package com.utefit.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConsistentFormValidator.class)
public @interface ConsistentForm {
    String message() default "Các trường trong biểu mẫu chưa nhất quán.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
