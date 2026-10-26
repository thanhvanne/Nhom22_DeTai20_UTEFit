package com.utefit.service;

import com.utefit.config.CloudinaryConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class PortableConfigurationTest {
    @Test void sharedConfigurationIsPortableAndContainsNoLiteralServiceCredentials() throws Exception {
        var properties = new Properties();
        try (var reader = Files.newBufferedReader(Path.of("src/main/resources/application.properties"))) { properties.load(reader); }
        assertTrue(properties.getProperty("spring.datasource.url").contains("databaseName=UTEFitDB"));
        assertFalse(Files.readString(Path.of("src/main/resources/application.properties")).contains("<<<<<<<"));
        assertFalse(Files.readString(Path.of("src/main/resources/application.properties")).matches("(?s).*(?<![A-Za-z])[A-Za-z]:[\\\\/].*"));
        assertEquals("validate", properties.getProperty("spring.jpa.hibernate.ddl-auto"));
        for (String key : new String[]{"cloudinary.api-secret", "cloudinary.api-key", "spring.datasource.password", "utefit.jwt.secret-base64"})
            assertTrue(properties.getProperty(key).startsWith("${"), key);
    }

    @Test void cloudinaryMayRemainUnconfiguredForOrdinaryLocalDemo() {
        var configuration = new CloudinaryConfig();
        ReflectionTestUtils.setField(configuration, "cloudName", "");
        ReflectionTestUtils.setField(configuration, "apiKey", "");
        ReflectionTestUtils.setField(configuration, "apiSecret", "");
        assertNotNull(configuration.cloudinary());
    }
}
