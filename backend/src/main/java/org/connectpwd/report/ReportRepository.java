package org.connectpwd.report;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportRepository extends MongoRepository<Report, String> {

    Optional<Report> findBySessionId(String sessionId);
}
