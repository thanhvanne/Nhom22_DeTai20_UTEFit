package com.utefit.service;

import com.utefit.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class DemoPasswordFixtureTest {
    @Test void producesAndVerifiesDevelopmentOnlyPasswordWithActualEncoder() throws Exception {
        var encoder = new SecurityConfig().passwordEncoder();
        String hash = encoder.encode("123456789");
        assertTrue(encoder.matches("123456789", hash));
        assertNotEquals("123456789", hash);
        Files.createDirectories(Path.of("target"));
        Files.writeString(Path.of("target/demo-password.hash"), hash);
    }
}
