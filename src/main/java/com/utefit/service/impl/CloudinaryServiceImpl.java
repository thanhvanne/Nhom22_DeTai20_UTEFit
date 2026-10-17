package com.utefit.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.utefit.dto.cloudinary.MediaUploadResult;
import com.utefit.service.CloudinaryService;
import com.utefit.validation.ImageValidator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;
    private final ImageValidator imageValidator;

    public CloudinaryServiceImpl(Cloudinary cloudinary,
                                 ImageValidator imageValidator) {
        this.cloudinary = cloudinary;
        this.imageValidator = imageValidator;
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