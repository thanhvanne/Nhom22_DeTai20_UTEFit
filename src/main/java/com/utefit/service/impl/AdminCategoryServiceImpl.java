package com.utefit.service.impl;

import com.utefit.dto.admin.ClassCategoryFormDto;
import com.utefit.entity.ClassCategory;
import com.utefit.repository.ClassCategoryRepository;
import com.utefit.service.AdminCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminCategoryServiceImpl implements AdminCategoryService {

    private final ClassCategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ClassCategory> getCategories(String keyword, Boolean active, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String searchPattern = (keyword != null && !keyword.trim().isEmpty())
                ? "%" + keyword.trim().toLowerCase() + "%"
                : null;
        return categoryRepository.searchCategories(searchPattern, active, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassCategory getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy loại lớp với ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public ClassCategoryFormDto getCategoryFormById(Long id) {
        ClassCategory c = getCategoryById(id);
        return ClassCategoryFormDto.builder()
                .id(c.getId())
                .requiredBenefit(c.getRequiredBenefit())
                .name(c.getName())
                .description(c.getDescription())
                .active(c.getActive())
                .build();
    }

    @Override
    @Transactional
    public void createCategory(ClassCategoryFormDto dto) {
        validateUniqueCategoryName(dto.getName(), null);

        ClassCategory category = ClassCategory.builder()
                .requiredBenefit(dto.getRequiredBenefit())
                .name(dto.getName().trim())
                .description(dto.getDescription() != null ? dto.getDescription().trim() : null)
                .active(dto.getActive() != null ? dto.getActive() : true)
                .build();

        categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void updateCategory(Long id, ClassCategoryFormDto dto) {
        ClassCategory category = getCategoryById(id);
        validateUniqueCategoryName(dto.getName(), id);

        category.setName(dto.getName().trim());
        category.setRequiredBenefit(dto.getRequiredBenefit());
        category.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        category.setActive(dto.getActive() != null ? dto.getActive() : true);

        categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        ClassCategory category = getCategoryById(id);
        category.setActive(!Boolean.TRUE.equals(category.getActive()));
        categoryRepository.save(category);
    }

    private void validateUniqueCategoryName(String rawName, Long currentId) {
        String name = rawName.trim();
        boolean duplicate = (currentId == null)
                ? categoryRepository.existsByNameIgnoreCase(name)
                : categoryRepository.existsByNameIgnoreCaseAndIdNot(name, currentId);

        if (duplicate) {
            throw new IllegalArgumentException("Tên loại lớp '" + name + "' đã tồn tại trong hệ thống!");
        }
    }
}
