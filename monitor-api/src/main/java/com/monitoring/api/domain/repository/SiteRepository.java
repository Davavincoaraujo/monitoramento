package com.monitoring.api.domain.repository;

import com.monitoring.api.domain.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {
    
    List<Site> findByEnabledTrue();
    
    @Query("""
        SELECT s FROM Site s 
        WHERE s.enabled = true 
        AND NOT EXISTS (
            SELECT r FROM Run r 
            WHERE r.site = s 
            AND r.startedAt > :since
        )
    """)
    List<Site> findSitesDueForCheck(LocalDateTime since);
}
