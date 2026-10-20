package com.eternalprotocol.api.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.eternalprotocol.api.exception.ImageUploadException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for uploading product photos to Cloudinary and deleting them again.
 * Cloudinary itself is mocked, so no real account or network is needed.
 */
@ExtendWith(MockitoExtension.class)
class ImageUploadServiceTest {

    @Mock
    private Cloudinary cloudinary;
    @Mock
    private Uploader uploader;

    @InjectMocks
    private ImageUploadService imageUploadService;

    @Test
    void uploadProductImage_validPng_returnsSecureUrlAndPublicId() throws IOException {
        // Given: a small PNG and a Cloudinary that accepts it
        MockMultipartFile file = new MockMultipartFile("file", "tee.png", "image/png", "fake-image-bytes".getBytes());
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/demo/image/upload/tee.png",
                "public_id", "eternal-protocol/products/tee123"));

        // When
        ImageUploadService.UploadResult result = imageUploadService.uploadProductImage(file);

        // Then: both the display URL and the ID needed for a later delete come back
        assertEquals("https://res.cloudinary.com/demo/image/upload/tee.png", result.url());
        assertEquals("eternal-protocol/products/tee123", result.publicId());
    }

    @Test
    void uploadProductImage_noFile_isRefused() {
        // When / Then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> imageUploadService.uploadProductImage(null));
        assertTrue(ex.getMessage().contains("No image file"));
    }

    @Test
    void uploadProductImage_emptyFile_isRefused() {
        // Given: a file with zero bytes
        MockMultipartFile empty = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        // When / Then
        assertThrows(IllegalArgumentException.class, () -> imageUploadService.uploadProductImage(empty));
    }

    @Test
    void uploadProductImage_overFiveMegabytes_isRefusedWithoutCallingCloudinary() {
        // Given: a 6MB image
        MockMultipartFile big = new MockMultipartFile("file", "big.png", "image/png", new byte[6 * 1024 * 1024]);

        // When / Then: refused up front, so no upload quota is wasted
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> imageUploadService.uploadProductImage(big));
        assertTrue(ex.getMessage().contains("5MB"));
        verifyNoInteractions(cloudinary);
    }

    @Test
    void uploadProductImage_nonImageFile_isRefusedWithoutCallingCloudinary() {
        // Given: a PDF
        MockMultipartFile pdf = new MockMultipartFile("file", "doc.pdf", "application/pdf", "not-an-image".getBytes());

        // When / Then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> imageUploadService.uploadProductImage(pdf));
        assertTrue(ex.getMessage().contains("JPEG"));
        verifyNoInteractions(cloudinary);
    }

    @Test
    void uploadProductImage_whenCloudinaryFails_throwsImageUploadException() throws IOException {
        // Given: Cloudinary cannot be reached
        MockMultipartFile file = new MockMultipartFile("file", "tee.png", "image/png", "fake-image-bytes".getBytes());
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenThrow(new IOException("Cloudinary unreachable"));

        // When / Then: wrapped so the API can answer 502 instead of a generic 500
        ImageUploadException ex = assertThrows(ImageUploadException.class,
                () -> imageUploadService.uploadProductImage(file));
        assertTrue(ex.getMessage().contains("Failed to upload"));
    }

    @Test
    void uploadProductImage_acceptsJpegPngWebpAndGif() throws IOException {
        // Given
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
                "secure_url", "https://example.com/img", "public_id", "id"));

        // When / Then: all four allowed types go through
        for (String contentType : new String[]{"image/jpeg", "image/png", "image/webp", "image/gif"}) {
            MockMultipartFile file = new MockMultipartFile("file", "img", contentType, "bytes".getBytes());
            assertEquals("https://example.com/img", imageUploadService.uploadProductImage(file).url());
        }
    }

    @Test
    void deleteProductImage_withPublicId_asksCloudinaryToDestroyIt() throws IOException {
        // Given
        when(cloudinary.uploader()).thenReturn(uploader);

        // When
        imageUploadService.deleteProductImage("eternal-protocol/products/tee123");

        // Then
        verify(uploader).destroy(eq("eternal-protocol/products/tee123"), anyMap());
    }

    @Test
    void deleteProductImage_withoutPublicId_doesNothing() {
        // When / Then: photos added by URL have no Cloudinary ID, so there is nothing to delete
        assertDoesNotThrow(() -> imageUploadService.deleteProductImage(null));
        assertDoesNotThrow(() -> imageUploadService.deleteProductImage("  "));
        verifyNoInteractions(cloudinary);
    }

    @Test
    void deleteProductImage_whenCloudinaryFails_doesNotBlockTheCaller() throws IOException {
        // Given: Cloudinary errors out during the delete
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(anyString(), anyMap())).thenThrow(new IOException("Cloudinary unreachable"));

        // When / Then: the database row is already gone by now, so the failure is only logged
        assertDoesNotThrow(() -> imageUploadService.deleteProductImage("some-id"));
    }
}
