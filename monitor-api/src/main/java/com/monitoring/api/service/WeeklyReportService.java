package com.monitoring.api.service;

import com.monitoring.api.domain.entity.PageResult;
import com.monitoring.api.domain.entity.Site;
import com.monitoring.api.domain.repository.*;
import com.monitoring.api.dto.report.*;
import com.monitoring.api.service.email.EmailSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WeeklyReportService {
    private static final Logger log = LoggerFactory.getLogger(WeeklyReportService.class);
    
    private final SiteRepository siteRepository;
    private final RunRepository runRepository;
    private final PageResultRepository pageResultRepository;
    private final FailureRepository failureRepository;
    private final RequestErrorRepository requestErrorRepository;
    private final EmailSenderService emailSender;
    private final TemplateEngine templateEngine;
    
    public WeeklyReportService(
            SiteRepository siteRepository,
            RunRepository runRepository,
            PageResultRepository pageResultRepository,
            FailureRepository failureRepository,
            RequestErrorRepository requestErrorRepository,
            EmailSenderService emailSender,
            TemplateEngine templateEngine) {
        this.siteRepository = siteRepository;
        this.runRepository = runRepository;
        this.pageResultRepository = pageResultRepository;
        this.failureRepository = failureRepository;
        this.requestErrorRepository = requestErrorRepository;
        this.emailSender = emailSender;
        this.templateEngine = templateEngine;
    }
    
    public void generateAndSendReports() {
        log.info("Starting weekly report generation");
        
        List<Site> sites = siteRepository.findByEnabledTrue();
        
        for (Site site : sites) {
            try {
                generateAndSendReport(site);
            } catch (Exception e) {
                log.error("Failed to generate report for site: {}", site.getName(), e);
            }
        }
        
        log.info("Completed weekly report generation for {} sites", sites.size());
    }
    
    private void generateAndSendReport(Site site) {
        log.info("Generating weekly report for site: {}", site.getName());
        
        LocalDate weekEnd = LocalDate.now();
        LocalDate weekStart = weekEnd.minusDays(7);
        LocalDate prevWeekStart = weekStart.minusDays(7);
        
        LocalDateTime from = weekStart.atStartOfDay();
        LocalDateTime to = weekEnd.atTime(23, 59, 59);
        LocalDateTime prevFrom = prevWeekStart.atStartOfDay();
        LocalDateTime prevTo = weekStart.atStartOfDay();
        
        // Calculate uptime
        Object[] uptimeData = runRepository.calculateUptime(site.getId(), from);
        Double uptime = calculateUptime(uptimeData);
        
        // Get failures by severity
        List<Object[]> severityCounts = failureRepository.countBySeveritySince(site.getId(), from);
        Map<String, Integer> failuresBySeverity = new HashMap<>();
        for (Object[] row : severityCounts) {
            failuresBySeverity.put(row[0].toString(), ((Number) row[1]).intValue());
        }
        
        // Get performance metrics
        List<PageResult> results = pageResultRepository.findPageResultsInRange(site.getId(), from, to);
        PerformanceData performance = calculatePerformance(results);
        
        List<PageResult> prevResults = pageResultRepository.findPageResultsInRange(site.getId(), prevFrom, prevTo);
        PerformanceData prevPerformance = calculatePerformance(prevResults);
        
        // Get top issues
        List<Object[]> issuesData = failureRepository.findTopRecurringIssues(site.getId(), from, 5);
        List<TopIssue> topIssues = issuesData.stream()
            .map(row -> new TopIssue(
                row[0].toString(),
                row[1].toString(),
                ((Number) row[2]).intValue()
            ))
            .collect(Collectors.toList());
        
        // Get slowest pages
        List<Object[]> pagesData = pageResultRepository.findSlowestPages(site.getId(), from, 5);
        List<SlowPage> slowestPages = pagesData.stream()
            .map(row -> new SlowPage(
                row[0].toString(),
                ((Number) row[1]).intValue()
            ))
            .collect(Collectors.toList());
        
        // Get top 404 assets
        List<Object[]> assetsData = requestErrorRepository.findTop404Assets(site.getId(), from, 10);
        List<AssetError> top404Assets = assetsData.stream()
            .map(row -> new AssetError(
                row[0].toString(),
                ((Number) row[1]).intValue()
            ))
            .collect(Collectors.toList());
        
        // Build report data
        WeeklyReportData reportData = new WeeklyReportData(
            site.getName(),
            weekStart,
            weekEnd,
            uptime,
            failuresBySeverity,
            performance,
            prevPerformance,
            topIssues,
            slowestPages,
            top404Assets,
            "http://localhost:8080/dashboard?siteId=" + site.getId()
        );
        
        // Render template
        Context context = new Context();
        context.setVariable("report", reportData);
        String htmlContent = templateEngine.process("weekly-report", context);
        
        // Send email
        if (site.getEmailRecipients() != null && !site.getEmailRecipients().isBlank()) {
            String[] recipients = site.getEmailRecipients().split(",");
            for (String recipient : recipients) {
                emailSender.sendHtmlEmail(
                    recipient.trim(),
                    "Weekly Monitoring Report - " + site.getName(),
                    htmlContent
                );
            }
            log.info("Sent weekly report for site: {} to {} recipients", 
                site.getName(), recipients.length);
        }
    }
    
    private Double calculateUptime(Object[] data) {
        if (data == null || data.length < 2) return 100.0;
        long total = ((Number) data[0]).longValue();
        long success = ((Number) data[1]).longValue();
        return total == 0 ? 100.0 : (success * 100.0 / total);
    }
    
    private PerformanceData calculatePerformance(List<PageResult> results) {
        List<Integer> loadTimes = results.stream()
            .map(PageResult::getLoadMs)
            .filter(Objects::nonNull)
            .sorted()
            .collect(Collectors.toList());
        
        List<Integer> ttfbTimes = results.stream()
            .map(PageResult::getTtfbMs)
            .filter(Objects::nonNull)
            .sorted()
            .collect(Collectors.toList());
        
        return new PerformanceData(
            percentile(loadTimes, 95),
            percentile(ttfbTimes, 95)
        );
    }
    
    private Integer percentile(List<Integer> values, int p) {
        if (values.isEmpty()) return null;
        int index = (int) Math.ceil(values.size() * p / 100.0) - 1;
        return values.get(Math.max(0, Math.min(index, values.size() - 1)));
    }
}
