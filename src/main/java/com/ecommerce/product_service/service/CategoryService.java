package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.CategoryRequestDTO;
import com.ecommerce.product_service.dto.CategoryResponseDTO;

import java.util.List;

public interface CategoryService {
    CategoryResponseDTO createCategory(CategoryRequestDTO dto);
    List<CategoryResponseDTO> getAllCategories();
    CategoryResponseDTO getCategoryById(Long id);
}