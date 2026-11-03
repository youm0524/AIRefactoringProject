package com.careerpick.dto;

import java.time.Instant;

public record CommunityRoomSummary(long id, long companyId, String companyName, Instant joinedAt,
                                   LastMessageView lastMessage) {}
