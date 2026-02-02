package com.monitoring.api.domain.repository;

import com.monitoring.api.domain.entity.RequestError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RequestErrorRepository extends JpaRepository<RequestError, Long> {
    
    List<RequestError> findByRunId(Long runId);
    
    @Query(value = """
        SELECT re.url, COUNT(*) as cnt
        FROM request_errors re
        JOIN runs r ON r.id = re.run_id
        WHERE r.site_id = :siteId
        AND re.status = 404
        AND r.started_at >= :from
        GROUP BY re.url
        ORDER BY cnt DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<Object[]> findTop404Assets(
        @Param("siteId") Long siteId,
        @Param("from") LocalDateTime from,
        @Param("limit") int limit
    );
    
    @Modifying
    @Query("DELETE FROM RequestError re WHERE re.createdAt < :cutoffDate")
    int deleteOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}
