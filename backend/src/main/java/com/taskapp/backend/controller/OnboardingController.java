package com.taskapp.backend.controller;

import com.taskapp.backend.dto.ApiResponse;
import com.taskapp.backend.dto.OnboardingResponse;
import com.taskapp.backend.service.OnboardingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {
    private final OnboardingService service;
    public OnboardingController(OnboardingService service) { this.service = service; }
    @GetMapping public ResponseEntity<ApiResponse<OnboardingResponse>> get(@RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(ApiResponse.success("Onboarding fetched", service.get(auth)));
    }
    @PostMapping("/skip") public ResponseEntity<ApiResponse<OnboardingResponse>> skip(@RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(ApiResponse.success("Onboarding skipped", service.skip(auth)));
    }
    @PostMapping("/guide/{action}") public ResponseEntity<ApiResponse<OnboardingResponse>> guide(@RequestHeader(value = "Authorization", required = false) String auth, @PathVariable String action) {
        return ResponseEntity.ok(ApiResponse.success("Guide updated", service.updateGuide(auth, action)));
    }
    @PostMapping("/demo-workspace") public ResponseEntity<ApiResponse<OnboardingResponse>> load(@RequestHeader(value = "Authorization", required = false) String auth, @RequestParam(defaultValue = "zh") String locale) {
        return ResponseEntity.ok(ApiResponse.success("Demo workspace loaded", service.loadDemo(auth, locale)));
    }
    @DeleteMapping("/demo-workspace") public ResponseEntity<ApiResponse<OnboardingResponse>> clear(@RequestHeader(value = "Authorization", required = false) String auth) {
        return ResponseEntity.ok(ApiResponse.success("Demo workspace cleared", service.clearDemo(auth)));
    }
}
