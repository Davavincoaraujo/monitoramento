package com.monitoring.api.domain.repository;

import com.monitoring.api.domain.entity.PageResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PageResultRepository extends JpaRepository<PageResult, Long> {
    
    List<PageResult> findByRunId(Long runId);
    
    @Query("""
        SELECT pr FROM PageResult pr
        JOIN pr.run r
        WHERE r.site.id = :siteId
        AND r.startedAt >= :from
        AND r.startedAt < :to
        ORDER BY r.startedAt
    """)
    List<PageResult> findPageResultsInRange(
        @Param("siteId") Long siteId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );
    
    @Query(value = """
        SELECT p.name, AVG(pr.load_ms) as avg_load
        FROM page_results pr
        JOIN site_pages p ON p.id = pr.page_id
        JOIN runs r ON r.id = pr.run_id
        WHERE r.site_id = :siteId
        AND r.started_at >= :from
        GROUP BY p.id, p.name
        ORDER BY avg_load DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<Object[]> findSlowestPages(
        @Param("siteId") Long siteId,
        @Param("from") LocalDateTime from,
        @Param("limit") int limit
    );
}
