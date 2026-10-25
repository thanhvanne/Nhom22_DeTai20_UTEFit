package com.utefit.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Service
public class QrImageService {
    public String dataUri(String value) {
        try {
            var matrix = new QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, 300, 300);
            BufferedImage image = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
            for (int row = 0; row < 300; row++) {
                for (int column = 0; column < 300; column++) {
                    image.setRGB(column, row, matrix.get(column, row) ? 0x000000 : 0xFFFFFF);
                }
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (com.google.zxing.WriterException | java.io.IOException ex) {
            throw new IllegalStateException("Không thể hiển thị QR. Hãy thử tạo lại mã.", ex);
        }
    }
}
