package com.utefit.service;

import com.utefit.dto.admin.ClassCategoryFormDto;
import com.utefit.entity.ClassCategory;
import org.springframework.data.domain.Page;

public interface AdminCategoryService {

    Page<ClassCategory> getCategories(String keyword, Boolean active, int page, int size);

    ClassCategory getCategoryById(Long id);

    ClassCategoryFormDto getCategoryFormById(Long id);

    void createCategory(ClassCategoryFormDto dto);

    void updateCategory(Long id, ClassCategoryFormDto dto);

    void toggleActive(Long id);
}