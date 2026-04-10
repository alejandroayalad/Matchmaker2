package com.alejandro.botjobhunter.service.pipeline;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PipelineScheduler {

    private final PipelineOrchestrator pipelineOrchestrator;

    public PipelineScheduler(PipelineOrchestrator pipelineOrchestrator) {
        this.pipelineOrchestrator = pipelineOrchestrator;
    }

    @Scheduled(cron = "${pipeline.cron:0 0 */6 * * *}")
    public void runScheduledPipeline() {
        System.out.println(">>> Pipeline triggered by scheduler.");
        pipelineOrchestrator.run();
    }
}