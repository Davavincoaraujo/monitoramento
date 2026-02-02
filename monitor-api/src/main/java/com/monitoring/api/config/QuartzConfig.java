package com.monitoring.api.config;

import com.monitoring.api.scheduler.CheckSchedulerJob;
import com.monitoring.api.scheduler.WeeklyReportJob;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {
    
    @Value("${monitoring.weekly-report-timezone:America/Sao_Paulo}")
    private String timezone;
    
    @Bean
    public JobDetail checkSchedulerJobDetail() {
        return JobBuilder.newJob(CheckSchedulerJob.class)
            .withIdentity("checkSchedulerJob")
            .storeDurably()
            .build();
    }
    
    @Bean
    public Trigger checkSchedulerTrigger() {
        // Run every minute to check for due sites
        return TriggerBuilder.newTrigger()
            .forJob(checkSchedulerJobDetail())
            .withIdentity("checkSchedulerTrigger")
            .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInMinutes(1)
                .repeatForever())
            .build();
    }
    
    @Bean
    public JobDetail weeklyReportJobDetail() {
        return JobBuilder.newJob(WeeklyReportJob.class)
            .withIdentity("weeklyReportJob")
            .storeDurably()
            .build();
    }
    
    @Bean
    public Trigger weeklyReportTrigger() {
        // Sunday at 20:00 in configured timezone
        return TriggerBuilder.newTrigger()
            .forJob(weeklyReportJobDetail())
            .withIdentity("weeklyReportTrigger")
            .withSchedule(CronScheduleBuilder
                .cronSchedule("0 0 20 ? * SUN")
                .inTimeZone(java.util.TimeZone.getTimeZone(timezone)))
            .build();
    }
}
