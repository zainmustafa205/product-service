package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.ProductRequestDTO;
import com.ecommerce.product_service.dto.ProductResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ProductService {
    ProductResponseDTO createProduct(ProductRequestDTO dto);
    ProductResponseDTO getProductById(Long id);
    Page<ProductResponseDTO> getProducts(Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    ProductResponseDTO updateProduct(Long id, ProductRequestDTO dto);
    void deleteProduct(Long id);
    void reduceStock(Long productId, Integer quantity);
    void restoreStock(Long productId, Integer quantity);
}