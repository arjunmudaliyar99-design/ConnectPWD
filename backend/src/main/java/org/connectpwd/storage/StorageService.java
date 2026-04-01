package org.connectpwd.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface StorageService {

    String uploadVoice(UUID sessionId, String questionCode, MultipartFile file);

    String uploadPdf(UUID sessionId, byte[] pdfBytes);

    String generatePresignedUrl(String objectKey);
}
