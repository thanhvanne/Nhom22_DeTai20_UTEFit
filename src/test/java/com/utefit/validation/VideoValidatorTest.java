package com.utefit.validation;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VideoValidatorTest {
    private final VideoValidator validator = new VideoValidator();

    private byte[] mp4() {
        return ByteBuffer.allocate(24).putInt(24).put("ftypisom".getBytes(StandardCharsets.US_ASCII))
                .putInt(0).put("isommp42".getBytes(StandardCharsets.US_ASCII)).array();
    }

    @Test
    void recognizesMp4ContainerBeforeProviderDecode() {
        assertDoesNotThrow(() -> validator.validate(new MockMultipartFile("video", "test.mp4", "video/mp4", mp4())));
    }

    @Test
    void recognizesWebmDocType() {
        byte[] data = {0x1a, 0x45, (byte) 0xdf, (byte) 0xa3, 0x42, (byte) 0x82, (byte) 0x84, 'w', 'e', 'b', 'm', 0};
        assertDoesNotThrow(() -> validator.validate(new MockMultipartFile("video", "test.webm", "video/webm", data)));
    }

    @Test
    void rejectsRenamedExecutableOrHtml() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(new MockMultipartFile("video", "test.mp4", "video/mp4", "<script>alert(1)</script>".getBytes())));
    }

    @Test
    void rejectsMimeAndExtensionMismatch() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(new MockMultipartFile("video", "test.mp4", "image/png", mp4())));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(new MockMultipartFile("video", "test.exe", "video/mp4", mp4())));
    }

    @Test
    void rejectsEmptyAndOversizedBeforeReading() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(new MockMultipartFile("video", new byte[0])));
        MultipartFile large = mock(MultipartFile.class);
        when(large.getSize()).thenReturn(VideoValidator.MAX_BYTES + 1);
        assertThrows(IllegalArgumentException.class, () -> validator.validate(large));
        verify(large, never()).getInputStream();
    }

    @Test
    void rejectsTruncatedFtypBox() {
        byte[] data = mp4();
        data[3] = 100;
        assertThrows(IllegalArgumentException.class, () -> validator.validate(new MockMultipartFile("video", "test.mp4", "video/mp4", data)));
    }

    @Test
    void acceptsUnderAndExactlyFiftyMiBAndRejectsOneByteOverBeforeReading() throws Exception {
        assertEquals(52_428_800L, VideoValidator.MAX_BYTES);
        for (long size : new long[]{VideoValidator.MAX_BYTES - 1, VideoValidator.MAX_BYTES}) {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getSize()).thenReturn(size);
            when(file.getContentType()).thenReturn("video/mp4");
            when(file.getOriginalFilename()).thenReturn("boundary.mp4");
            when(file.getInputStream()).thenReturn(new ByteArrayInputStream(mp4()));
            assertDoesNotThrow(() -> validator.validate(file));
        }
        MultipartFile oversized = mock(MultipartFile.class);
        when(oversized.getSize()).thenReturn(VideoValidator.MAX_BYTES + 1);
        var error = assertThrows(IllegalArgumentException.class, () -> validator.validate(oversized));
        assertTrue(error.getMessage().contains("50 MiB"));
        verify(oversized, never()).getInputStream();
    }
}
