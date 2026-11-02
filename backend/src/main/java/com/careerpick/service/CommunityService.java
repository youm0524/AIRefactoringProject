package com.careerpick.service;

import com.careerpick.dto.CommunityRoomView;
import com.careerpick.dto.JoinCommunityResponse;
import com.careerpick.entity.CommunityRoom;
import com.careerpick.entity.Job;
import com.careerpick.entity.User;
import com.careerpick.repository.CommunityRoomMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommunityService {
    private final AuthService authService;
    private final JobService jobService;
    private final CommunityRoomMemberRepository members;

    public CommunityService(AuthService authService, JobService jobService, CommunityRoomMemberRepository members) {
        this.authService = authService;
        this.jobService = jobService;
        this.members = members;
    }

    @Transactional
    public JoinCommunityResponse join(long jobId, String authorization) {
        User user = authService.requireUserEntity(authorization);
        Job job = jobService.findById(jobId);
        if (job == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "공고를 찾을 수 없습니다.");

        CommunityRoom room = job.communityRoom();
        members.join(user.id(), room.id());
        return new JoinCommunityResponse(new CommunityRoomView(room.id(), room.companyName()));
    }
}
