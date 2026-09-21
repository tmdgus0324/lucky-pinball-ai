package com.luckypinball.common;

import java.time.Instant;

public record ErrorLogEntry(Instant timestamp, String path, String message) {
}
