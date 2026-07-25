package org.nors.dev.codes.lpu.dto;

import java.time.Instant;
import org.nors.dev.codes.lpu.model.EventTone;

public record EventToneResponse(
        String id,
        String url,
        String originalName,
        String contentType,
        long sizeBytes,
        Instant uploadedAt
) {
    public static EventToneResponse from(EventTone tone) {
        return new EventToneResponse(
                String.valueOf(tone.getId()),
                tone.getFilePath(),
                tone.getOriginalName(),
                tone.getContentType(),
                tone.getSizeBytes(),
                tone.getUploadedAt()
        );
    }
}
