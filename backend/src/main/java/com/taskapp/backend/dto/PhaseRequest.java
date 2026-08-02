package com.taskapp.backend.dto;

import com.taskapp.backend.model.PhaseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PhaseRequest {

    @Size(max = 120, message = "phaseKey must be at most 120 characters")
    private String phaseKey;

    @Size(max = 120, message = "parentPhaseKey must be at most 120 characters")
    private String parentPhaseKey;

    @NotBlank(message = "phaseName is required")
    @Size(max = 100, message = "phaseName must be at most 100 characters")
    private String phaseName;

    @Size(max = 2000, message = "phaseDescription must be at most 2000 characters")
    private String phaseDescription;

    @NotNull(message = "phaseStatus is required")
    private PhaseStatus phaseStatus;

    public String getPhaseKey() {
        return phaseKey;
    }

    public void setPhaseKey(String phaseKey) {
        this.phaseKey = phaseKey;
    }

    public String getParentPhaseKey() {
        return parentPhaseKey;
    }

    public void setParentPhaseKey(String parentPhaseKey) {
        this.parentPhaseKey = parentPhaseKey;
    }

    public String getPhaseName() {
        return phaseName;
    }

    public void setPhaseName(String phaseName) {
        this.phaseName = phaseName;
    }

    public PhaseStatus getPhaseStatus() {
        return phaseStatus;
    }

    public void setPhaseStatus(PhaseStatus phaseStatus) {
        this.phaseStatus = phaseStatus;
    }

    public String getPhaseDescription() {
        return phaseDescription;
    }

    public void setPhaseDescription(String phaseDescription) {
        this.phaseDescription = phaseDescription;
    }
}
