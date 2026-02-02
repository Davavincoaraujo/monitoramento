package com.monitoring.api.domain.repository;

import com.monitoring.api.domain.entity.Failure;
import com.monitoring.api.domain.enums.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FailureRepository extends JpaRepository<Failure, Long> {
    
    List<Failure> findByRunId(Long runId);
    
    @Query("""
        SELECT f.severity, COUNT(f)
        FROM Failure f
        JOIN f.run r
        WHERE r.site.id = :siteId
        AND r.startedAt >= :from
        GROUP BY f.severity
    """)
    List<Object[]> countBySeveritySince(
        @Param("siteId") Long siteId,
        @Param("from") LocalDateTime from
    );
    
    @Query(value = """
        SELECT f.type, f.message, COUNT(*) as cnt
        FROM failures f
        JOIN runs r ON r.id = f.run_id
        WHERE r.site_id = :siteId
        AND r.started_at >= :from
        GROUP BY f.type, f.message
        ORDER BY cnt DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<Object[]> findTopRecurringIssues(
        @Param("siteId") Long siteId,
        @Param("from") LocalDateTime from,
        @Param("limit") int limit
    );
}
