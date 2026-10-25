package com.utefit.service;

import com.utefit.dto.cloudinary.MediaUploadResult;
import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {

   
    MediaUploadResult uploadImage(MultipartFile file, String folder);

    MediaUploadResult uploadVideo(MultipartFile file, String folder);

    void deleteVideo(String publicId);

   
    void delete(String publicId);
}
