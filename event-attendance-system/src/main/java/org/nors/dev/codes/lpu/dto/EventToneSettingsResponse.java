package org.nors.dev.codes.lpu.dto;

import java.util.List;
import java.util.Map;

public record EventToneSettingsResponse(
        List<EventToneResponse> tones,
        Map<String, String> assignments
) {}
