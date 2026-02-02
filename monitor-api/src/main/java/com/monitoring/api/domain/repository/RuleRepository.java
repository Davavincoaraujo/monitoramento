package com.monitoring.api.domain.repository;

import com.monitoring.api.domain.entity.Rule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RuleRepository extends JpaRepository<Rule, Long> {
    
    List<Rule> findBySiteId(Long siteId);
    
    List<Rule> findByPageId(Long pageId);
}
