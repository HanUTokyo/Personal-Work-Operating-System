package com.taskapp.backend.dto;

public record OnboardingResponse(String status, boolean hasDemoData, boolean guideClosed, boolean projectDone, boolean focusDone, boolean knowledgeDone, boolean aiDone) { }
