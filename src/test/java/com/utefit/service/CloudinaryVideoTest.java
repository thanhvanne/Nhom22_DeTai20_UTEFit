package com.utefit.service;

import com.cloudinary.Cloudinary;
import com.utefit.service.impl.CloudinaryServiceImpl;
import com.utefit.validation.ImageValidator;
import com.utefit.validation.VideoValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CloudinaryVideoTest {
    private final Cloudinary cloud = mock(Cloudinary.class, RETURNS_DEEP_STUBS);
    private final CloudinaryServiceImpl service = new CloudinaryServiceImpl(cloud, new ImageValidator(), new VideoValidator());
    private MockMultipartFile video;
    private Map<String, Object> result;

    @BeforeEach
    void setup() {
        byte[] bytes = ByteBuffer.allocate(24).putInt(24).put("ftypisom".getBytes(StandardCharsets.US_ASCII))
                .putInt(0).put("isommp42".getBytes(StandardCharsets.US_ASCII)).array();
        video = new MockMultipartFile("video", "qa.mp4", "video/mp4", bytes);
        result = new HashMap<>(Map.of("public_id", "qa-video", "secure_url", "https://res.cloudinary.com/qa/video/upload/qa.mp4",
                "resource_type", "video", "format", "mp4", "width", 640, "height", 480, "duration", 2.5, "bytes", 1024));
    }

    @Test
    void uploadsAsVideoAndChecksProviderDecodedMetadata() throws Exception {
        when(cloud.uploader().upload(any(byte[].class), anyMap())).thenReturn(result);
        assertEquals("qa-video", service.uploadVideo(video, "utefit/reviews/videos").publicId());
        verify(cloud.uploader()).upload(any(byte[].class), argThat(options -> "video".equals(options.get("resource_type"))
                && Boolean.FALSE.equals(options.get("overwrite"))));
    }

    @Test
    void audioOrUndecodableContainerIsRejectedAndCleanedUp() throws Exception {
        result.put("width", 0);
        when(cloud.uploader().upload(any(byte[].class), anyMap())).thenReturn(result);
        assertThrows(IllegalArgumentException.class, () -> service.uploadVideo(video, "qa"));
        verify(cloud.uploader()).destroy(eq("qa-video"), argThat(options -> "video".equals(options.get("resource_type"))));
    }

    @Test
    void cloudinaryFailureDoesNotReturnUrl() throws Exception {
        when(cloud.uploader().upload(any(byte[].class), anyMap())).thenThrow(new IOException("QA offline"));
        assertThrows(IllegalStateException.class, () -> service.uploadVideo(video, "qa"));
    }

    @Test
    void renamedFileIsRejectedBeforeUpload() throws Exception {
        var fake = new MockMultipartFile("video", "qa.mp4", "video/mp4", "not a video".getBytes());
        assertThrows(IllegalArgumentException.class, () -> service.uploadVideo(fake, "qa"));
        verify(cloud.uploader(), never()).upload(any(byte[].class), anyMap());
    }

    @Test
    void decodedVideoMetadataAllowsFiftyMiBButRejectsLargerAndCleansOnlyNewAsset() throws Exception {
        result.put("bytes", VideoValidator.MAX_BYTES);
        when(cloud.uploader().upload(any(byte[].class), anyMap())).thenReturn(result);
        assertEquals("qa-video", service.uploadVideo(video, "qa").publicId());
        verify(cloud.uploader(), never()).destroy(anyString(), anyMap());
        result.put("bytes", VideoValidator.MAX_BYTES + 1);
        assertThrows(IllegalArgumentException.class, () -> service.uploadVideo(video, "qa"));
        verify(cloud.uploader()).destroy(eq("qa-video"), anyMap());
    }
}
