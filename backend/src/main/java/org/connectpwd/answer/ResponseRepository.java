package org.connectpwd.answer;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResponseRepository extends MongoRepository<ResponseDocument, String> {

    List<ResponseDocument> findBySessionIdAndLevel(String sessionId, int level);

    Optional<ResponseDocument> findBySessionIdAndQuestionCode(String sessionId, String questionCode);

    long countBySessionIdAndLevel(String sessionId, int level);

    List<ResponseDocument> findBySessionId(String sessionId);
}
