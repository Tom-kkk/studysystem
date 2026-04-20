package com.example.stu_backend.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DeadlineReminderScheduler {

    private final ExperimentMailService experimentMailService;

    public DeadlineReminderScheduler(ExperimentMailService experimentMailService) {
        this.experimentMailService = experimentMailService;
    }

    @Scheduled(cron = "${app.mail.deadline-reminder-cron:0 0/30 * * * ?}")
    public void sendDeadlineReminderTask() {
        experimentMailService.sendDeadlineReminders();
    }
}
