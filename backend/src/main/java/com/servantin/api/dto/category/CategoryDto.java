package com.servantin.api.dto.category;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CategoryDto {
    private UUID id;
    private String slug;
    private String name;
    private String description;
    private String icon;
    private Integer sortOrder;
}
