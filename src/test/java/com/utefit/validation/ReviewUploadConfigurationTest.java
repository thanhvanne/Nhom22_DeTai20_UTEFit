package com.utefit.validation;

import com.utefit.config.ReviewMultipartConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.util.unit.DataSize;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class ReviewUploadConfigurationTest {
    @Test
    void baseAndSepayProfileAgreeOnBinaryFiftyAndSeventyMiB() throws Exception {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(Path.of("src/main/resources/application.properties"))) {
            properties.load(input);
        }
        for (String profile : new String[]{"application-qa.properties", "application-sepay.properties"}) {
            Properties merged = new Properties();
            merged.putAll(properties);
            try (InputStream input = Files.newInputStream(Path.of("src/main/resources", profile))) { merged.load(input); }
            assertEquals(VideoValidator.MAX_BYTES, DataSize.parse(merged.getProperty("spring.servlet.multipart.max-file-size")).toBytes());
            assertEquals(70L * 1024 * 1024, DataSize.parse(merged.getProperty("spring.servlet.multipart.max-request-size")).toBytes());
        }
    }

    @Test
    void beanUsesBoundPropertiesInsteadOfHardcodedOldLimitAndImageRemainsFiveMiB() {
        MultipartProperties properties = new MultipartProperties();
        properties.setMaxFileSize(DataSize.ofMegabytes(50));
        properties.setMaxRequestSize(DataSize.ofMegabytes(70));
        var config = new ReviewMultipartConfig().multipartConfigElement(properties);
        assertEquals(VideoValidator.MAX_BYTES, config.getMaxFileSize());
        properties.setMaxRequestSize(DataSize.ofMegabytes(71));
        assertEquals(71L * 1024 * 1024, new ReviewMultipartConfig().multipartConfigElement(properties).getMaxRequestSize());
        var image = org.mockito.Mockito.mock(org.springframework.web.multipart.MultipartFile.class);
        org.mockito.Mockito.when(image.getSize()).thenReturn(5L * 1024 * 1024 + 1);
        assertThrows(IllegalArgumentException.class, () -> new ImageValidator().validate(image));
    }
}
