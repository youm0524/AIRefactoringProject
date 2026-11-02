package com.careerpick.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
public record CreateCoverLetterRequest(@NotNull @Positive Long jobId) {}
