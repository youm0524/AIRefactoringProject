package com.careerpick.dto;

import java.time.Instant;

public record ChatMessage(long id, long roomId, String author, String text, Instant createdAt,
                          boolean mine, String clientMessageId) {}
