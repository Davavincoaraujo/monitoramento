package com.monitoring.api.domain.repository;

import com.monitoring.api.domain.entity.Run;
import com.monitoring.api.domain.enums.RunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RunRepository extends JpaRepository<Run, Long> {
    
    List<Run> findBySiteIdOrderByStartedAtDesc(Long siteId);
    
    List<Run> findBySiteIdAndStartedAtBetweenOrderByStartedAtDesc(
        Long siteId, LocalDateTime from, LocalDateTime to);
    
    @Query("""
        SELECT r FROM Run r 
        WHERE r.site.id = :siteId 
        AND r.startedAt >= :from 
        AND r.startedAt < :to 
        ORDER BY r.startedAt DESC
    """)
    List<Run> findRunsInRange(
        @Param("siteId") Long siteId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );
    
    @Query("""
        SELECT COUNT(r) as total,
               SUM(CASE WHEN r.status = 'SUCCESS' THEN 1 ELSE 0 END) as success
        FROM Run r
        WHERE r.site.id = :siteId
        AND r.startedAt >= :from
    """)
    Object[] calculateUptime(@Param("siteId") Long siteId, @Param("from") LocalDateTime from);
    
    @Query("""
        SELECT r.status, COUNT(r) 
        FROM Run r 
        WHERE r.site.id = :siteId 
        AND r.startedAt >= :from 
        GROUP BY r.status
    """)
    List<Object[]> countByStatusSince(@Param("siteId") Long siteId, @Param("from") LocalDateTime from);
}
