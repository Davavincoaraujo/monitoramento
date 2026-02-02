package com.monitoring.api.controller;

import com.monitoring.api.domain.entity.Site;
import com.monitoring.api.domain.entity.SitePage;
import com.monitoring.api.domain.repository.SitePageRepository;
import com.monitoring.api.domain.repository.SiteRepository;
import com.monitoring.api.dto.api.PageConfigDTO;
import com.monitoring.api.dto.api.SiteConfigResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sites")
public class SiteController {
    
    private final SiteRepository siteRepository;
    private final SitePageRepository pageRepository;
    
    public SiteController(SiteRepository siteRepository, SitePageRepository pageRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
    }
    
    @GetMapping("/{id}/config")
    public ResponseEntity<SiteConfigResponse> getSiteConfig(@PathVariable Long id) {
        Site site = siteRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Site not found: " + id));
        
        List<SitePage> pages = pageRepository.findBySiteIdAndEnabledTrue(id);
        
        List<PageConfigDTO> pageConfigs = pages.stream()
            .map(p -> new PageConfigDTO(p.getId(), p.getName(), p.getPath()))
            .collect(Collectors.toList());
        
        SiteConfigResponse response = new SiteConfigResponse(
            site.getId(),
            site.getName(),
            site.getBaseUrl(),
            pageConfigs
        );
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<List<Site>> getAllSites() {
        return ResponseEntity.ok(siteRepository.findAll());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Site> getSite(@PathVariable Long id) {
        return siteRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
