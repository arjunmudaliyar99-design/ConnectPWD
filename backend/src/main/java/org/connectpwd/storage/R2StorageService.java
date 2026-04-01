package org.connectpwd.storage;

import lombok.extern.slf4j.Slf4j;
import org.connectpwd.common.AppException;
import org.connectpwd.common.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class R2StorageService implements StorageService {

    @Value("${r2.account-id}")
    private String accountId;

    @Value("${r2.access-key-id}")
    private String accessKeyId;

    @Value("${r2.secret-access-key}")
    private String secretAccessKey;

    @Value("${r2.bucket-name}")
    private String bucketName;

    private S3Client s3Client;
    private S3Presigner presigner;

    @PostConstruct
    public void init() {
        if (accountId == null || accountId.isBlank()) {
            log.warn("R2 storage not configured — uploads will fail");
            return;
        }

        URI endpoint = URI.create("https://" + accountId + ".r2.cloudflarestorage.com");
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);

        this.s3Client = S3Client.builder()
                .endpointOverride(endpoint)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto"))
                .build();

        this.presigner = S3Presigner.builder()
                .endpointOverride(endpoint)
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of("auto"))
                .build();
    }

    @Override
    public String uploadVoice(UUID sessionId, String questionCode, MultipartFile file) {
        String key = "voice/" + sessionId + "/" + questionCode + ".webm";
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("audio/webm")
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));
            log.info("Uploaded voice recording: {}", key);
            return key;
        } catch (IOException e) {
            throw new AppException(ErrorCode.STORAGE_ERROR, "Failed to upload voice recording",
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public String uploadPdf(UUID sessionId, byte[] pdfBytes) {
        String key = "reports/" + sessionId + "/report.pdf";
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("application/pdf")
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(pdfBytes));
        log.info("Uploaded PDF report: {}", key);
        return key;
    }

    @Override
    public String generatePresignedUrl(String objectKey) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(getRequest)
                .build();

        return presigner.presignGetObject(presignRequest).url().toString();
    }
}
