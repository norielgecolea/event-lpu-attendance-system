package org.nors.dev.codes.lpu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record EventRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must be at most 255 characters")
        String title,

        String description,

        @Size(max = 255, message = "Location must be at most 255 characters")
        String location,

        @NotNull(message = "Start time is required")
        Instant startsAt,

        Instant endsAt,

        Boolean active
) {
}
