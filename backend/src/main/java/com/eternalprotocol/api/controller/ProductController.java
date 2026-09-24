package com.eternalprotocol.api.controller;

import com.eternalprotocol.api.dto.ProductDto;
import com.eternalprotocol.api.dto.ProductRequestDto;
import com.eternalprotocol.api.entity.ProductCategory;
import com.eternalprotocol.api.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for browsing the storefront and, for admins, managing
 * products.
 * <p>
 * Read endpoints (GET) are public so anyone can browse the shop. Write
 * endpoints (POST/PUT/DELETE) are restricted to the ADMIN role — see the
 * rules in {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    /**
     * @param productService product CRUD and search logic
     */
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Public. Lists products, optionally filtered. Powers both the
     * category pages (MEN'S/WOMEN'S/ACCESSORIES with colour/size filters)
     * and the product detail page's variant lookup. Any filter left out is
     * simply not applied.
     *
     * @param colour      colour to filter by, optional
     * @param size        size to filter by, optional
     * @param styleCode   style code to filter by, optional
     * @param category    top-level category to filter by, optional
     * @param subCategory subcategory to filter by, optional
     * @return matching products, newest first
     */
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts(
            @RequestParam(required = false) String colour,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String styleCode,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) String subCategory) {
        return ResponseEntity.ok(productService.getAllProducts(colour, size, styleCode, category, subCategory));
    }

    /**
     * Public. Distinct subcategories that currently have products within
     * one category — powers the navbar's per-category dropdown. Only ever
     * lists subcategories that actually exist right now, the same way the
     * shop page's colour/size filter options work.
     *
     * @param category category to look up subcategories for
     * @return distinct subcategory names
     */
    @GetMapping("/subcategories")
    public ResponseEntity<List<String>> getSubCategories(@RequestParam ProductCategory category) {
        return ResponseEntity.ok(productService.getSubCategories(category));
    }

    /**
     * Public. Gets a single product variant by ID.
     *
     * @param id product variant's ID
     * @return the matching product
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    /**
     * Admin only. Creates a new product variant.
     *
     * @param request new product's details
     * @return the created product
     */
    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductRequestDto request) {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    /**
     * Admin only. Updates an existing product variant.
     *
     * @param id      product variant's ID
     * @param request updated product details
     * @return the updated product
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequestDto request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    /**
     * Admin only. Permanently deletes a product variant. Fails if the
     * product is referenced by any past order — see
     * {@code ProductInUseException}.
     *
     * @param id product variant's ID
     * @return empty 204 response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
