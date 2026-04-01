package org.connectpwd.consent;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.connectpwd.common.AuditLog;
import org.connectpwd.consent.dto.ConsentRequest;
import org.connectpwd.consent.dto.ConsentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsentService {

    private final ConsentRepository consentRepository;
    private final AuditLog auditLog;

    @Transactional
    public ConsentResponse createConsent(UUID userId, ConsentRequest request, String ipAddress) {
        Consent consent = Consent.builder()
                .userId(userId)
                .clientName(request.getClientName())
                .clientDob(request.getClientDob())
                .legalName(request.getLegalName())
                .relationship(request.getRelationship())
                .agreed(request.getAgreed())
                .ipAddress(ipAddress)
                .build();

        consent = consentRepository.save(consent);
        auditLog.logConsentSigned(userId, ipAddress);

        return toResponse(consent);
    }

    public Consent findById(UUID id) {
        return consentRepository.findById(id).orElse(null);
    }

    private ConsentResponse toResponse(Consent consent) {
        return ConsentResponse.builder()
                .id(consent.getId())
                .clientName(consent.getClientName())
                .clientDob(consent.getClientDob())
                .legalName(consent.getLegalName())
                .relationship(consent.getRelationship())
                .agreed(consent.isAgreed())
                .signedAt(consent.getSignedAt())
                .build();
    }
}
