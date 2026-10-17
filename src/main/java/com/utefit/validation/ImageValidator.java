package com.utefit.validation;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;

@Component
public class ImageValidator {

    private static final long MAX_FILE_SIZE =
            5L * 1024 * 1024;

    private static final Set<String> ALLOWED_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/png",
                    "image/gif"
            );

    private static final Set<String> ALLOWED_FORMATS =
            Set.of(
                    "JPEG",
                    "PNG",
                    "GIF"
            );

    public void validate(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Vui lòng chọn ảnh để tải lên"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Dung lượng ảnh không được vượt quá 5MB"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null ||
                !ALLOWED_TYPES.contains(
                        contentType.toLowerCase()
                )) {

            throw new IllegalArgumentException(
                    "Chỉ hỗ trợ ảnh JPG, PNG và GIF"
            );
        }

        try {

            byte[] data = file.getBytes();

            try (var input = ImageIO.createImageInputStream(
                    new ByteArrayInputStream(data)
            )) {

                if (input == null) {
                    throw new IllegalArgumentException(
                            "Không thể đọc dữ liệu ảnh"
                    );
                }

                var readers = ImageIO.getImageReaders(input);

                if (!readers.hasNext()) {
                    throw new IllegalArgumentException(
                            "File không phải ảnh hợp lệ"
                    );
                }

                var reader = readers.next();

                try {

                    String format =
                            reader.getFormatName().toUpperCase();

                    if (!ALLOWED_FORMATS.contains(format)) {
                        throw new IllegalArgumentException(
                                "Định dạng ảnh không được hỗ trợ"
                        );
                    }

                    reader.setInput(input);

                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);

                    if (width <= 0 || height <= 0 ||
                            width > 10000 || height > 10000) {

                        throw new IllegalArgumentException(
                                "Kích thước ảnh không hợp lệ"
                        );
                    }

                    boolean matches = switch (format) {
                        case "JPEG" -> contentType.equalsIgnoreCase("image/jpeg");
                        case "PNG" -> contentType.equalsIgnoreCase("image/png");
                        case "GIF" -> contentType.equalsIgnoreCase("image/gif");
                        default -> false;
                    };

                    if (!matches) {
                        throw new IllegalArgumentException(
                                "Định dạng ảnh không khớp với loại file"
                        );
                    }

                } finally {
                    reader.dispose();
                }
            }

        } catch (IOException e) {

            throw new IllegalArgumentException(
                    "Không thể kiểm tra file ảnh",
                    e
            );
        }
    }
}