package com.utefit.cloudinary;

import com.utefit.dto.cloudinary.MediaUploadResult;
import com.utefit.service.CloudinaryService;
import com.utefit.validation.ImageValidator;

import com.utefit.config.CloudinaryConfig;
import com.utefit.service.impl.CloudinaryServiceImpl;
import com.utefit.validation.VideoValidator;

import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockMultipartFile;

import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import org.junit.jupiter.api.Assumptions;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(classes = {
        CloudinaryConfig.class,
        CloudinaryServiceImpl.class,
        ImageValidator.class,
        VideoValidator.class
})
@TestPropertySource(properties = {
        "CLOUDINARY_CLOUD_NAME=${CLOUDINARY_CLOUD_NAME_ENV:}",
        "CLOUDINARY_API_KEY=${CLOUDINARY_API_KEY_ENV:}",
        "CLOUDINARY_API_SECRET=${CLOUDINARY_API_SECRET_ENV:}"
})
class CloudinaryIntegrationTest {

    @Autowired
    private CloudinaryService cloudinaryService;

    @Test
    void uploadImageSuccessfully() throws Exception {

        String imagePath =
                System.getenv("CLOUDINARY_TEST_IMAGE_PATH");

        Assumptions.assumeTrue(
                imagePath != null && !imagePath.isBlank(),
                "Chưa cấu hình CLOUDINARY_TEST_IMAGE_PATH"
        );

        Assumptions.assumeTrue(
                hasCloudinaryCredentials(),
                "Chưa cấu hình Cloudinary credentials"
        );

        Path path = Path.of(imagePath);

        Assumptions.assumeTrue(
                Files.isRegularFile(path),
                "Không tìm thấy ảnh kiểm thử"
        );

        String filename = path.getFileName().toString();

        String contentType =
                Files.probeContentType(path);

        if (contentType == null) {
            contentType = "image/jpeg";
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                filename,
                contentType,
                Files.readAllBytes(path)
        );

        MediaUploadResult result =
                cloudinaryService.uploadImage(
                        file,
                        "utefit/trainers"
                );

        assertNotNull(result);
        assertNotNull(result.url());
        assertNotNull(result.publicId());

        assertTrue(
                result.url().startsWith("https://")
        );

        assertTrue(
                result.publicId().startsWith("utefit/trainers/")
        );

        System.out.println("UPLOAD SUCCESS");
        System.out.println("URL: " + result.url());
        System.out.println("PUBLIC ID: " + result.publicId());
    }

    private boolean hasCloudinaryCredentials() {

        return hasEnv("CLOUDINARY_CLOUD_NAME_ENV")
                && hasEnv("CLOUDINARY_API_KEY_ENV")
                && hasEnv("CLOUDINARY_API_SECRET_ENV");
    }

    private boolean hasEnv(String name) {

        String value = System.getenv(name);

        return value != null && !value.isBlank();
    }
}
