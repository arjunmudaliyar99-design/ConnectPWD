package org.connectpwd.scoring;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IsaaScoreRepository extends JpaRepository<IsaaScore, UUID> {

    Optional<IsaaScore> findBySessionId(UUID sessionId);

    boolean existsBySessionId(UUID sessionId);
}
