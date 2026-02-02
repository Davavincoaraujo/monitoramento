package com.monitoring.api.scheduler;

import com.monitoring.api.service.WeeklyReportService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Component
public class WeeklyReportJob implements Job {
    
    private final WeeklyReportService weeklyReportService;
    
    public WeeklyReportJob(WeeklyReportService weeklyReportService) {
        this.weeklyReportService = weeklyReportService;
    }
    
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        weeklyReportService.generateAndSendReports();
    }
}
