package com.careerpick.controller;

import com.careerpick.dto.JoinCommunityResponse;
import com.careerpick.service.CommunityService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

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
}
