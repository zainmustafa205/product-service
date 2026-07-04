package com.ecommerce.product_service.dto;

import lombok.*;

@Getter
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class CategoryResponseDTO {
    private Long id;
    private String name;
    private String description;
}