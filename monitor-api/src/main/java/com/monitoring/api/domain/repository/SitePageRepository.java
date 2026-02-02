package com.monitoring.api.domain.repository;

import com.monitoring.api.domain.entity.SitePage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SitePageRepository extends JpaRepository<SitePage, Long> {
    
    List<SitePage> findBySiteIdAndEnabledTrue(Long siteId);
    
    List<SitePage> findBySiteId(Long siteId);
}
