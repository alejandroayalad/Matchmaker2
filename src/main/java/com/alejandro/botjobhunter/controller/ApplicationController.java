package com.alejandro.botjobhunter.controller;

import com.alejandro.botjobhunter.models.Application;
import com.alejandro.botjobhunter.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationRepository applicationRepository;

    @GetMapping
    public List<Application> getAll() {
        return applicationRepository.findAll();
    }

    @PostMapping
    public Application create(@RequestBody Application application) {
        return applicationRepository.save(application);
    }

    @PutMapping("/{id}")
    public Application update(@PathVariable Long id, @RequestBody Application application) {
        application.setId(id);
        return applicationRepository.save(application);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        applicationRepository.deleteById(id);
    }
}