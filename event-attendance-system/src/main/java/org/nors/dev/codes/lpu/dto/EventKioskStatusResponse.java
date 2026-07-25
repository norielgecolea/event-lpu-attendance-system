package org.nors.dev.codes.lpu.dto;

import java.util.List;
import java.util.Map;

public record EventKioskStatusResponse(
        boolean active,
        int kioskCount,
        List<String> activeEventIds,
        Map<String, Integer> kioskCounts
) {}
