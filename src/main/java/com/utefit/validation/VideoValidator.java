package com.utefit.validation;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

@Component
public class VideoValidator {
    public static final long MAX_BYTES = 50L * 1024 * 1024;

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("Video phải có dữ liệu và không vượt quá 50 MiB (52.428.800 byte).");
        }
        String mime = file.getContentType();
        String name = file.getOriginalFilename();
        if (mime == null || name == null) {
            throw new IllegalArgumentException("Thiếu loại hoặc tên file video.");
        }
        mime = mime.toLowerCase(Locale.ROOT);
        name = name.toLowerCase(Locale.ROOT);
        try (var input = file.getInputStream()) {
            byte[] header = input.readNBytes(4096);
            boolean mp4 = mime.equals("video/mp4") && name.endsWith(".mp4") && mp4(header, file.getSize());
            boolean webm = mime.equals("video/webm") && name.endsWith(".webm") && webm(header);
            if (!mp4 && !webm) {
                throw new IllegalArgumentException("Chỉ nhận MP4/WebM có MIME, phần mở rộng và nội dung khớp nhau.");
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Không đọc được video.", ex);
        }
    }

    private boolean mp4(byte[] header, long length) {
        if (header.length < 24 || !new String(header, 4, 4, StandardCharsets.US_ASCII).equals("ftyp")) {
            return false;
        }
        long boxSize = Integer.toUnsignedLong(ByteBuffer.wrap(header, 0, 4).getInt());
        if (boxSize < 16 || boxSize > header.length || boxSize > length) {
            return false;
        }
        Set<String> brands = Set.of("isom", "iso2", "iso4", "iso5", "iso6", "mp41", "mp42", "avc1", "M4V ", "dash");
        for (int offset = 8; offset + 4 <= boxSize; offset += 4) {
            if (offset != 12 && brands.contains(new String(header, offset, 4, StandardCharsets.US_ASCII))) {
                return true;
            }
        }
        return false;
    }

    private boolean webm(byte[] header) {
        if (header.length < 12 || (header[0] & 255) != 0x1a || (header[1] & 255) != 0x45
                || (header[2] & 255) != 0xdf || (header[3] & 255) != 0xa3) {
            return false;
        }
        for (int offset = 4; offset + 7 <= header.length; offset++) {
            if ((header[offset] & 255) == 0x42 && (header[offset + 1] & 255) == 0x82
                    && (header[offset + 2] & 255) == 0x84
                    && new String(header, offset + 3, 4, StandardCharsets.US_ASCII).equals("webm")) {
                return true;
            }
        }
        return false;
    }
}
