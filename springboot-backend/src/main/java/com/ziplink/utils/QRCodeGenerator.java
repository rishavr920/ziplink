package com.ziplink.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * QRCodeGenerator utility class.
 * 
 * Uses Google's ZXing barcode processing library to generate high-quality QR code
 * matrixes, render them to PNG image bytes, and output base64 encoded Data URIs.
 */
public class QRCodeGenerator {

    private static final int WIDTH = 200;
    private static final int HEIGHT = 200;

    /**
     * Generates a 200x200 QR code representing the text, formatted as a base64 PNG data URL.
     * 
     * @param text The payload to embed inside the QR code (the shortUrl).
     * @return Base64 data-uri string (e.g. data:image/png;base64,iVBORw0KGgo...)
     */
    public static String generateQRCode(String text) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M); // Medium level error tolerance
        hints.put(EncodeHintType.MARGIN, 1); // 1-cell border padding

        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, WIDTH, HEIGHT, hints);

        try (ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngBytes = pngOutputStream.toByteArray();
            
            String base64Image = Base64.getEncoder().encodeToString(pngBytes);
            return "data:image/png;base64," + base64Image;
        }
    }
}
