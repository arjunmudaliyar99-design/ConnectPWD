package org.connectpwd.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String uploadVoice(String sessionId, String questionCode, MultipartFile file);

    String uploadPdf(String sessionId, byte[] pdfBytes);

    String generatePresignedUrl(String objectKey);
}
