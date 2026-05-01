package org.connectpwd.scoring;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IsaaScoreRepository extends MongoRepository<IsaaScore, String> {

    Optional<IsaaScore> findBySessionId(String sessionId);

    boolean existsBySessionId(String sessionId);
}
