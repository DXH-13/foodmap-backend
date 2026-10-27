package com.foodmap.category.web;

import com.foodmap.category.CategoryService;
import com.foodmap.category.dto.CategoryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "category", description = "Danh mục món ăn")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /** Mảng trần, không phân trang — đúng hợp đồng: danh mục là tập nhỏ, cố định. */
    @GetMapping("/api/v1/categories")
    @Operation(operationId = "listCategories", summary = "Lấy toàn bộ danh mục món ăn")
    public List<CategoryDto> listCategories() {
        return categoryService.listAll();
    }
}
