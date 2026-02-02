package com.monitoring.api.scheduler;

import com.monitoring.api.service.CheckSchedulerService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Component
public class CheckSchedulerJob implements Job {
    
    private final CheckSchedulerService checkSchedulerService;
    
    public CheckSchedulerJob(CheckSchedulerService checkSchedulerService) {
        this.checkSchedulerService = checkSchedulerService;
    }
    
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        checkSchedulerService.scheduleChecks();
    }
}
