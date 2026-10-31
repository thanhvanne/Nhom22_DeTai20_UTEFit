package com.utefit.validation;

import java.util.LinkedHashMap;
import java.util.Map;

public class InputViolationException extends IllegalArgumentException {
    private final Map<String, String> fieldErrors;

    public InputViolationException(Map<String, String> fieldErrors) {
        super("Vui lòng kiểm tra các trường được đánh dấu.");
        this.fieldErrors = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
