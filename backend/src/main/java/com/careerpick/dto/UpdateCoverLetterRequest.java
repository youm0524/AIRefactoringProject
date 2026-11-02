package com.careerpick.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record UpdateCoverLetterRequest(@NotEmpty List<@NotNull @Valid Item> items) {
    public record Item(@NotNull @Positive Long id, @NotNull String answer) {}
}
