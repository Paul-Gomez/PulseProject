package com.pulse.workspaces.mapper;

import com.pulse.workspaces.dto.MemberResponse;
import com.pulse.workspaces.entity.WorkspaceMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MemberMapper {

    @Mapping(target = "roleName", source = "role.name")
    MemberResponse toResponse(WorkspaceMember member);
}
