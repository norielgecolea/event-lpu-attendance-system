package org.nors.dev.codes.lpu.dto;

import java.time.Instant;
import java.time.ZoneId;

public record ServerTimeResponse(Instant now, String zone) {
    private static final ZoneId CAMPUS_ZONE = ZoneId.of("Asia/Manila");

    public static ServerTimeResponse current() {
        return new ServerTimeResponse(Instant.now(), CAMPUS_ZONE.getId());
    }
}
