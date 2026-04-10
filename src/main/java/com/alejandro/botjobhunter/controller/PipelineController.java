package com.alejandro.botjobhunter.controller;

import com.alejandro.botjobhunter.dto.PipelineResult;
import com.alejandro.botjobhunter.service.pipeline.PipelineOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pipeline")
@RequiredArgsConstructor
public class PipelineController {

    private final PipelineOrchestrator pipelineOrchestrator;

    @PostMapping("/run")
    public PipelineResult runPipeline() {
        return pipelineOrchestrator.run();
    }
}