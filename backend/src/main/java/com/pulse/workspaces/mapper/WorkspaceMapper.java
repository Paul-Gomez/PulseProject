package com.pulse.workspaces.mapper;

import com.pulse.workspaces.dto.WorkspaceResponse;
import com.pulse.workspaces.entity.Workspace;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorkspaceMapper {

    WorkspaceResponse toResponse(Workspace workspace);
}
