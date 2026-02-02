package com.monitoring.api.controller;

import com.monitoring.api.config.RabbitMQConfig;
import com.monitoring.api.domain.entity.Site;
import com.monitoring.api.domain.entity.SitePage;
import com.monitoring.api.domain.repository.SitePageRepository;
import com.monitoring.api.domain.repository.SiteRepository;
import com.monitoring.api.dto.api.CreateSiteRequest;
import com.monitoring.api.dto.api.PageConfigDTO;
import com.monitoring.api.dto.api.SiteConfigResponse;
import com.monitoring.api.dto.message.RunCheckMessage;
import jakarta.validation.Valid;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller para gerenciamento de Sites monitorados.
 * 
 * <p>Fornece endpoints para:</p>
 * <ul>
 *   <li>CRUD completo de sites</li>
 *   <li>Configuração de páginas a serem monitoradas</li>
 *   <li>Trigger manual de checks via RabbitMQ</li>
 *   <li>Parsing de URLs completas (baseUrl + path)</li>
 * </ul>
 * 
 * <p><b>Endpoints principais:</b></p>
 * <pre>
 * GET    /api/sites           - Lista todos os sites
 * GET    /api/sites/{id}      - Busca site por ID
 * POST   /api/sites           - Cria novo site
 * PUT    /api/sites/{id}      - Atualiza site
 * DELETE /api/sites/{id}      - Remove site
 * POST   /api/sites/{id}/check - Executa check manual
 * </pre>
 * 
 * @author Sistema de Monitoramento
 * @version 1.0
 * @since 2026-02-02
 */
@RestController
@RequestMapping("/api/sites")
public class SiteController {
    
    private final SiteRepository siteRepository;
    private final SitePageRepository pageRepository;
    private final RabbitTemplate rabbitTemplate;
    
    public SiteController(SiteRepository siteRepository, SitePageRepository pageRepository, RabbitTemplate rabbitTemplate) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.rabbitTemplate = rabbitTemplate;
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
    
    @PostMapping
    public ResponseEntity<Site> createSite(@Valid @RequestBody CreateSiteRequest request) {
        // Parse full URL to extract baseUrl and path
        UrlParts urlParts = parseUrl(request.baseUrl());
        
        // Check if site already exists with same base URL
        Site existingSite = siteRepository.findAll().stream()
            .filter(s -> s.getBaseUrl().equals(urlParts.baseUrl()))
            .findFirst()
            .orElse(null);
        
        if (existingSite != null) {
            // Check if page already exists, if not create it
            if (!urlParts.path().equals("/")) {
                boolean pageExists = pageRepository.findBySiteIdAndEnabledTrue(existingSite.getId())
                    .stream()
                    .anyMatch(p -> p.getPath().equals(urlParts.path()));
                
                if (!pageExists) {
                    SitePage newPage = new SitePage();
                    newPage.setSite(existingSite);
                    newPage.setName(extractPageName(urlParts.path()));
                    newPage.setPath(urlParts.path());
                    newPage.setEnabled(true);
                    pageRepository.save(newPage);
                }
            }
            return ResponseEntity.ok(existingSite);
        }
        
        Site site = new Site();
        site.setName(request.name());
        site.setBaseUrl(urlParts.baseUrl());
        site.setEnabled(request.enabled());
        site.setFrequencySeconds(request.frequencySeconds() != null ? request.frequencySeconds() : 300);
        
        Site savedSite = siteRepository.save(site);
        
        // Create page with the specified path
        SitePage defaultPage = new SitePage();
        defaultPage.setSite(savedSite);
        defaultPage.setName(extractPageName(urlParts.path()));
        defaultPage.setPath(urlParts.path());
        defaultPage.setEnabled(true);
        pageRepository.save(defaultPage);
        
        return ResponseEntity.ok(savedSite);
    }
    
    @PostMapping("/{id}/check")
    public ResponseEntity<Void> triggerCheck(@PathVariable Long id) {
        Site site = siteRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Site not found: " + id));
        
        RunCheckMessage message = new RunCheckMessage(
            site.getId(),
            site.getName(),
            site.getBaseUrl()
        );
        
        rabbitTemplate.convertAndSend(RabbitMQConfig.RUN_CHECK_QUEUE, message);
        
        return ResponseEntity.accepted().build();
    }
    
    private UrlParts parseUrl(String url) {
        // Ensure https:// prefix
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        
        try {
            java.net.URL parsedUrl = new java.net.URL(url);
            String baseUrl = parsedUrl.getProtocol() + "://" + parsedUrl.getHost();
            if (parsedUrl.getPort() != -1 && parsedUrl.getPort() != 80 && parsedUrl.getPort() != 443) {
                baseUrl += ":" + parsedUrl.getPort();
            }
            
            String path = parsedUrl.getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }
            
            return new UrlParts(baseUrl, path);
        } catch (Exception e) {
            // Fallback: treat entire URL as baseUrl
            return new UrlParts(url, "/");
        }
    }
    
    private String extractPageName(String path) {
        if (path.equals("/")) {
            return "Home";
        }
        
        // Extract filename or last segment
        String[] segments = path.split("/");
        String lastSegment = segments[segments.length - 1];
        
        // Remove extension and capitalize
        String name = lastSegment.replaceAll("\\.[^.]+$", "");
        if (name.isEmpty()) {
            return "Page";
        }
        
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }
    
    private record UrlParts(String baseUrl, String path) {}
}
