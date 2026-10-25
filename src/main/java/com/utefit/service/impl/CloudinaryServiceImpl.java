package com.utefit.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.utefit.dto.cloudinary.MediaUploadResult;
import com.utefit.service.CloudinaryService;
import com.utefit.validation.ImageValidator;
import com.utefit.validation.VideoValidator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;
    private final ImageValidator imageValidator;
    private final VideoValidator videoValidator;

    public CloudinaryServiceImpl(Cloudinary cloudinary,
                                 ImageValidator imageValidator, VideoValidator videoValidator) {
        this.cloudinary = cloudinary;
        this.imageValidator = imageValidator;
        this.videoValidator = videoValidator;
    }

    @Override
    public MediaUploadResult uploadImage(MultipartFile file, String folder) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File ảnh không được để trống");
        }

        // Kiểm tra định dạng, kích thước, MIME type...
        imageValidator.validate(file);

        try {
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "image",
                    "overwrite", true
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader()
                    .upload(file.getBytes(), uploadParams);

            String url = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");

            if (url == null || publicId == null) {
                throw new IllegalStateException("Upload ảnh lên Cloudinary thất bại");
            }

            return new MediaUploadResult(url, publicId);

        } catch (IOException e) {
            throw new RuntimeException("Lỗi IO khi upload ảnh lên Cloudinary", e);
        }
    }

    @Override
    public MediaUploadResult uploadVideo(MultipartFile file, String folder) {
        videoValidator.validate(file);
        String publicId = null;
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", folder, "resource_type", "video", "overwrite", false,
                    "allowed_formats", java.util.List.of("mp4", "webm")));
            publicId = (String) result.get("public_id");
            String url = (String) result.get("secure_url");
            if (publicId == null || publicId.isBlank() || publicId.length() > 255 || url == null
                    || !url.startsWith("https://res.cloudinary.com/") || url.length() > 500
                    || !"video".equals(result.get("resource_type"))
                    || !java.util.Set.of("mp4", "webm").contains(String.valueOf(result.get("format")))
                    || !(result.get("width") instanceof Number width) || width.intValue() <= 0
                    || !(result.get("height") instanceof Number height) || height.intValue() <= 0
                    || !(result.get("duration") instanceof Number duration) || !Double.isFinite(duration.doubleValue()) || duration.doubleValue() <= 0
                    || !(result.get("bytes") instanceof Number bytes) || bytes.longValue() <= 0 || bytes.longValue() > VideoValidator.MAX_BYTES) {
                throw new IllegalArgumentException("Cloudinary không xác nhận được video MP4/WebM hợp lệ.");
            }
            return new MediaUploadResult(url, publicId);
        } catch (IOException | RuntimeException ex) {
            if (publicId != null) {
                try {
                    deleteVideo(publicId);
                } catch (RuntimeException cleanup) {
                    ex.addSuppressed(cleanup);
                }
            }
            if (ex instanceof IllegalArgumentException invalid) {
                throw invalid;
            }
            throw new IllegalStateException("Không thể tải video lên Cloudinary. Thử lại sau.", ex);
        }
    }

    @Override
    public void deleteVideo(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "video", "invalidate", true));
        } catch (IOException ex) {
            throw new IllegalStateException("Không xóa được video Cloudinary.", ex);
        }
    }

    @Override
    public void delete(String publicId) {

        if (publicId == null || publicId.isBlank()) {
            return;
        }

        try {
            Map<String, Object> params = ObjectUtils.asMap(
                    "invalidate", true
            );

            cloudinary.uploader().destroy(publicId, params);

        } catch (IOException e) {
            throw new RuntimeException("Lỗi IO khi xoá ảnh trên Cloudinary", e);
        }
    }
}
