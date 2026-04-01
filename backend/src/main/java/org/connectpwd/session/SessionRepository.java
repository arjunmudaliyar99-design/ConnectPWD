package org.connectpwd.session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<AssessmentSession, UUID> {

    List<AssessmentSession> findByUserId(UUID userId);

    boolean existsByConsentId(UUID consentId);
}
