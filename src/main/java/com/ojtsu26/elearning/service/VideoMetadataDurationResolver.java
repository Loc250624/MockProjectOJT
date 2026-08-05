package com.ojtsu26.elearning.service;

import java.util.OptionalInt;

public interface VideoMetadataDurationResolver {
    OptionalInt resolveDurationSeconds(String videoUrl);
}
