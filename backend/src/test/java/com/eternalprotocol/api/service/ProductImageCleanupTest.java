package com.eternalprotocol.api.service;

import com.eternalprotocol.api.entity.ProductImage;
import com.eternalprotocol.api.exception.ResourceNotFoundException;
import com.eternalprotocol.api.repository.ProductImageRepository;
import com.eternalprotocol.api.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests that deleting a gallery photo also removes its file from Cloudinary.
 */
@ExtendWith(MockitoExtension.class)
class ProductImageCleanupTest {

    @Mock
    private ProductImageRepository productImageRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ImageUploadService imageUploadService;

    @InjectMocks
    private ProductImageService productImageService;

    private ProductImage image(Long id, String publicId, int sortOrder, boolean cover) {
        ProductImage image = new ProductImage();
        image.setId(id);
        image.setStyleCode("STYLE-1");
        image.setColour("Black");
        image.setImageUrl("https://cdn/" + id + ".png");
        image.setCloudinaryPublicId(publicId);
        image.setSortOrder(sortOrder);
        image.setCover(cover);
        return image;
    }

    @Test
    void deleteImage_removesTheDatabaseRowAndTheCloudinaryFile() {
        // Given: a non-cover photo that was uploaded to Cloudinary
        ProductImage photo = image(2L, "pub_2", 1, false);
        when(productImageRepository.findById(2L)).thenReturn(Optional.of(photo));
        when(productImageRepository.findByStyleCodeAndColourAndCoverTrue("STYLE-1", "Black"))
                .thenReturn(Optional.of(image(1L, "pub_1", 0, true)));
        when(productRepository.search("STYLE-1", "Black", null)).thenReturn(List.of());

        // When
        productImageService.deleteImage(2L);

        // Then: both the row and the Cloudinary asset are gone
        verify(productImageRepository).delete(photo);
        verify(imageUploadService).deleteProductImage("pub_2");
    }

    @Test
    void deleteImage_whenCoverPhotoIsDeleted_nextPhotoBecomesCover() {
        // Given: the cover photo and one other photo
        ProductImage cover = image(1L, "pub_1", 0, true);
        ProductImage next = image(2L, "pub_2", 1, false);
        when(productImageRepository.findById(1L)).thenReturn(Optional.of(cover));
        when(productImageRepository.findByStyleCodeAndColourOrderBySortOrderAsc("STYLE-1", "Black"))
                .thenReturn(List.of(next));
        when(productImageRepository.findByStyleCodeAndColourAndCoverTrue("STYLE-1", "Black"))
                .thenReturn(Optional.of(next));
        when(productRepository.search("STYLE-1", "Black", null)).thenReturn(List.of());

        // When
        productImageService.deleteImage(1L);

        // Then: the Cloudinary file is removed and the next photo is promoted
        verify(imageUploadService).deleteProductImage("pub_1");
        assertTrue(next.isCover());
        verify(productImageRepository).save(next);
    }

    @Test
    void deleteImage_whenPhotoDoesNotExist_throwsAndTouchesNothingOnCloudinary() {
        // Given
        when(productImageRepository.findById(404L)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(ResourceNotFoundException.class, () -> productImageService.deleteImage(404L));
        verifyNoInteractions(imageUploadService);
    }

    @Test
    void deleteAllImagesForStyleAndColour_removesEveryRowAndEveryCloudinaryFile() {
        // Given: two photos for one colour
        ProductImage first = image(1L, "pub_1", 0, true);
        ProductImage second = image(2L, "pub_2", 1, false);
        when(productImageRepository.findByStyleCodeAndColourOrderBySortOrderAsc("STYLE-1", "Black"))
                .thenReturn(List.of(first, second));

        // When
        productImageService.deleteAllImagesForStyleAndColour("STYLE-1", "Black");

        // Then
        verify(productImageRepository).deleteAll(List.of(first, second));
        verify(imageUploadService).deleteProductImage("pub_1");
        verify(imageUploadService).deleteProductImage("pub_2");
    }

    @Test
    void deleteAllImagesForStyleAndColour_withNoPhotos_doesNothing() {
        // Given
        when(productImageRepository.findByStyleCodeAndColourOrderBySortOrderAsc("STYLE-1", "Black"))
                .thenReturn(List.of());

        // When
        productImageService.deleteAllImagesForStyleAndColour("STYLE-1", "Black");

        // Then
        verify(productImageRepository, never()).deleteAll(any());
        verifyNoInteractions(imageUploadService);
    }
}
