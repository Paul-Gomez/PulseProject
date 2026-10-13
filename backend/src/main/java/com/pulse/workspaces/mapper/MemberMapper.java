package com.pulse.workspaces.mapper;

import com.pulse.users.dto.UserSummaryResponse;
import com.pulse.workspaces.dto.MemberResponse;
import com.pulse.workspaces.entity.WorkspaceMember;
import org.springframework.stereotype.Component;

/** Hand-written because a member response combines data from two modules (membership + user profile). */
@Component
public class MemberMapper {

    public MemberResponse toResponse(WorkspaceMember member, UserSummaryResponse user) {
        return new MemberResponse(member.getId(), member.getUserId(), member.getRole().getName(),
                member.getJoinedAt(), user);
    }
}
