package com.taskapp.backend.dto;

import com.taskapp.backend.model.PhaseStatus;

public class PhaseResponse {

    private Long id;
    private String phaseKey;
    private String parentPhaseKey;
    private String phaseName;
    private String phaseDescription;
    private PhaseStatus phaseStatus;
    private int sortOrder;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getPhaseDescription() {
        return phaseDescription;
    }

    public void setPhaseDescription(String phaseDescription) {
        this.phaseDescription = phaseDescription;
    }

    public PhaseStatus getPhaseStatus() {
        return phaseStatus;
    }

    public void setPhaseStatus(PhaseStatus phaseStatus) {
        this.phaseStatus = phaseStatus;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
