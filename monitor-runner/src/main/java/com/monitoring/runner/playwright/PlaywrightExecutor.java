package com.monitoring.runner.playwright;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.monitoring.runner.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class PlaywrightExecutor {
    private static final Logger log = LoggerFactory.getLogger(PlaywrightExecutor.class);
    
    @Value("${playwright.headless:true}")
    private boolean headless;
    
    @Value("${playwright.timeout-ms:30000}")
    private int timeoutMs;
    
    @Value("${playwright.viewport-width:1920}")
    private int viewportWidth;
    
    @Value("${playwright.viewport-height:1080}")
    private int viewportHeight;
    
    public IngestRunRequest executeCheck(SiteConfig siteConfig) {
        log.info("Starting check for site: {} ({})", siteConfig.name(), siteConfig.siteId());
        
        IngestRunRequest request = new IngestRunRequest();
        request.setSiteId(siteConfig.siteId());
        request.setStartedAt(LocalDateTime.now());
        
        List<PageResultDTO> allPageResults = new ArrayList<>();
        List<FailureDTO> allFailures = new ArrayList<>();
        List<RequestErrorDTO> allRequestErrors = new ArrayList<>();
        
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(headless));
            
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(viewportWidth, viewportHeight));
            
            for (PageConfig pageConfig : siteConfig.pages()) {
                try {
                    PageExecutionResult result = executePage(context, siteConfig.baseUrl(), pageConfig);
                    
                    allPageResults.add(result.pageResult);
                    allFailures.addAll(result.failures);
                    allRequestErrors.addAll(result.requestErrors);
                } catch (Exception e) {
                    log.error("Failed to execute page: {}", pageConfig.name(), e);
                    
                    // Add critical failure
                    FailureDTO failure = new FailureDTO(
                        pageConfig.pageId(),
                        "CRITICAL",
                        "NAVIGATION_FAILED",
                        "Failed to navigate to page: " + e.getMessage(),
                        siteConfig.baseUrl() + pageConfig.path()
                    );
                    allFailures.add(failure);
                }
            }
            
            browser.close();
        } catch (Exception e) {
            log.error("Fatal error during check execution for site: {}", siteConfig.name(), e);
            request.setStatus("FAILED");
            request.setSummary("Fatal error: " + e.getMessage());
            request.setEndedAt(LocalDateTime.now());
            return request;
        }
        
        request.setPageResults(allPageResults);
        request.setFailures(allFailures);
        request.setRequestErrors(allRequestErrors);
        request.setEndedAt(LocalDateTime.now());
        
        // Determine status
        long criticalCount = allFailures.stream()
            .filter(f -> "CRITICAL".equals(f.getSeverity()))
            .count();
        long majorCount = allFailures.stream()
            .filter(f -> "MAJOR".equals(f.getSeverity()))
            .count();
        
        if (criticalCount > 0) {
            request.setStatus("FAILED");
        } else if (majorCount > 0) {
            request.setStatus("WARNING");
        } else {
            request.setStatus("SUCCESS");
        }
        
        request.setSummary(String.format(
            "Completed %d pages, %d critical, %d major issues",
            allPageResults.size(), criticalCount, majorCount
        ));
        
        log.info("Check completed for site: {}, status: {}", siteConfig.name(), request.getStatus());
        
        return request;
    }
    
    private PageExecutionResult executePage(BrowserContext context, String baseUrl, PageConfig pageConfig) {
        log.debug("Executing page: {}", pageConfig.name());
        
        Page page = context.newPage();
        
        NetworkCollector networkCollector = new NetworkCollector();
        ConsoleCollector consoleCollector = new ConsoleCollector();
        PerfCollector perfCollector = new PerfCollector();
        
        // Setup listeners
        page.onRequest(request -> networkCollector.onRequest());
        
        page.onResponse(response -> networkCollector.onResponse(response));
        
        page.onRequestFailed(request -> 
            networkCollector.onRequestFailed(request.url(), request.failure())
        );
        
        page.onConsoleMessage(message -> consoleCollector.onConsoleMessage(message));
        
        page.onPageError(error -> consoleCollector.onPageError(error));
        
        // Navigate
        String fullUrl = baseUrl + pageConfig.path();
        page.navigate(fullUrl, new Page.NavigateOptions().setTimeout(timeoutMs));
        
        // Wait for load
        page.waitForLoadState(LoadState.LOAD);
        
        // Collect performance metrics
        perfCollector.collectMetrics(page);
        
        // Build result
        PageResultDTO pageResult = new PageResultDTO();
        pageResult.setPageId(pageConfig.pageId());
        pageResult.setFinalUrl(page.url());
        pageResult.setTtfbMs(perfCollector.getTtfbMs());
        pageResult.setDomMs(perfCollector.getDomMs());
        pageResult.setLoadMs(perfCollector.getLoadMs());
        pageResult.setRequestsCount(networkCollector.getRequestCount());
        pageResult.setTotalBytes(networkCollector.getTotalBytes());
        
        List<FailureDTO> failures = new ArrayList<>();
        failures.addAll(networkCollector.getFailures());
        failures.addAll(consoleCollector.getFailures());
        
        // Set pageId on all failures
        for (FailureDTO failure : failures) {
            failure.setPageId(pageConfig.pageId());
        }
        
        page.close();
        
        return new PageExecutionResult(pageResult, failures, networkCollector.getRequestErrors());
    }
    
    private record PageExecutionResult(
        PageResultDTO pageResult,
        List<FailureDTO> failures,
        List<RequestErrorDTO> requestErrors
    ) {}
}
