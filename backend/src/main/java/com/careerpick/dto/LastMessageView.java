package com.careerpick.dto;

import java.time.Instant;

public record LastMessageView(long id, String author, String text, Instant createdAt) {}
