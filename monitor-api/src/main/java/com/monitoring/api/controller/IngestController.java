package com.monitoring.api.controller;

import com.monitoring.api.dto.ingest.IngestRunRequest;
import com.monitoring.api.dto.ingest.IngestRunResponse;
import com.monitoring.api.service.IngestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ingest")
public class IngestController {
    
    private final IngestService ingestService;
    
    public IngestController(IngestService ingestService) {
        this.ingestService = ingestService;
    }
    
    @PostMapping("/runs")
    public ResponseEntity<IngestRunResponse> ingestRun(@Valid @RequestBody IngestRunRequest request) {
        IngestRunResponse response = ingestService.ingestRun(request);
        return ResponseEntity.ok(response);
    }
}
