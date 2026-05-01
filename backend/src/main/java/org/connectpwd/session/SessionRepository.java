package org.connectpwd.session;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public interface SessionRepository extends MongoRepository<AssessmentSession, String> {

    List<AssessmentSession> findByUserId(String userId);

    @NonNull
    Page<AssessmentSession> findAll(@NonNull Pageable pageable);

    long countByStatus(SessionStatus status);

    @Aggregation(pipeline = {
        "{ $match: { user_id: { $in: ?0 } } }",
        "{ $group: { _id: '$user_id', count: { $sum: 1 } } }"
    })
    List<SessionCountResult> countSessionsByUserIdIn(List<String> userIds);

    default Map<String, Long> countByUserIdIn(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();
        return countSessionsByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(
                        SessionCountResult::id,
                        SessionCountResult::count
                ));
    }

    record SessionCountResult(String id, long count) {}
}
