package com.careerpick.controller;

import com.careerpick.dto.ChatMessage;
import com.careerpick.dto.CommunityRoomSummary;
import com.careerpick.dto.JoinCommunityResponse;
import com.careerpick.service.CommunityService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class CommunityController {
    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) { this.communityService = communityService; }

    @PutMapping("/jobs/{jobId}/community-membership")
    @SecurityRequirement(name = "bearerAuth")
    public JoinCommunityResponse join(@PathVariable long jobId,
                                      @RequestHeader(name = "Authorization", required = false) String authorization) {
        return communityService.join(jobId, authorization);
    }

    @DeleteMapping("/community-rooms/{roomId}/membership")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    public void leave(@PathVariable long roomId,
                      @RequestHeader(name = "Authorization", required = false) String authorization) {
        communityService.leave(roomId, authorization);
    }

    @GetMapping("/community-rooms")
    @SecurityRequirement(name = "bearerAuth")
    public List<CommunityRoomSummary> rooms(
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        return communityService.rooms(authorization);
    }

    @GetMapping("/community-rooms/{roomId}/messages")
    @SecurityRequirement(name = "bearerAuth")
    public List<ChatMessage> messages(@PathVariable long roomId,
                                      @RequestHeader(name = "Authorization", required = false) String authorization) {
        return communityService.messages(roomId, authorization);
    }
}
