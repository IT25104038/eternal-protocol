package com.eternalprotocol.api.service;

import com.eternalprotocol.api.dto.ProductDto;
import com.eternalprotocol.api.dto.ProductRequestDto;
import com.eternalprotocol.api.entity.Product;
import com.eternalprotocol.api.entity.ProductCategory;
import com.eternalprotocol.api.entity.ProductStatus;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.factory.product.ProductCreatorFactory;
import com.eternalprotocol.api.repository.CartItemRepository;
import com.eternalprotocol.api.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the product catalogue: creating products and searching the shop.
 */
@ExtendWith(MockitoExtension.class)
class ShopAndProductCatalogTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductImageService productImageService;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        // The real ProductCreatorFactory is used because it holds the size rules we want to test
        productService = new ProductService(
                productRepository, cartItemRepository, productImageService, new ProductCreatorFactory());
    }

    @Test
    void createProduct_accessoryWithoutSize_defaultsToOneSize() {
        // Given: an accessory (a cap) with no size given
        ProductRequestDto request = new ProductRequestDto(
                "CAP-01", "Team Cap", ProductCategory.ACCESSORIES, "Caps",
                new BigDecimal("2500.00"), "Black", null, 20, null, "A team cap", null, null);
        when(productRepository.save(any(Product.class))).thenAnswer(call -> call.getArgument(0));

        // When
        ProductDto result = productService.createProduct(request);

        // Then: size becomes "One Size" and the product is live in the shop by default
        assertEquals("One Size", result.size());
        assertEquals(ProductStatus.ACTIVE, result.status());
    }

    @Test
    void createProduct_apparelWithoutSize_throwsAndSavesNothing() {
        // Given: a clothing item (men's) with no size given
        ProductRequestDto request = new ProductRequestDto(
                "HOOD-01", "Team Hoodie", ProductCategory.MENS, "Hoodies",
                new BigDecimal("8500.00"), "Black", null, 10, null, "A hoodie", null, null);

        // When / Then: clothing must always have a size
        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(request));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void getAllProducts_returnsOnlyWhatTheRepositoryFindsForTheFilters() {
        // Given: the repository finds one black men's product that is ACTIVE
        Product hoodie = new Product("HOOD-01", "Team Hoodie", ProductCategory.MENS, "Hoodies",
                new BigDecimal("8500.00"), "Black", "M", 10, null, "A hoodie");
        when(productRepository.searchStorefront(
                null, "Black", null, ProductCategory.MENS, null, ProductStatus.ACTIVE))
                .thenReturn(List.of(hoodie));

        // When: the shop page asks for black, men's, active products
        List<ProductDto> result = productService.getAllProducts(
                "Black", null, null, ProductCategory.MENS, null, ProductStatus.ACTIVE);

        // Then
        assertEquals(1, result.size());
        assertEquals("Team Hoodie", result.get(0).name());
    }

    @Test
    void getProductById_whenProductDoesNotExist_throwsNotFound() {
        // Given: no product with this ID
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(ResourceNotFoundException.class, () -> productService.getProductById(99L));
    }
}
