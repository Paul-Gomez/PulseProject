package com.pulse.workspaces.controller;

import com.pulse.common.security.CurrentUserProvider;
import com.pulse.workspaces.dto.CreateWorkspaceRequest;
import com.pulse.workspaces.dto.UpdateWorkspaceRequest;
import com.pulse.workspaces.dto.WorkspaceResponse;
import com.pulse.workspaces.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceResponse create(@Valid @RequestBody CreateWorkspaceRequest request) {
        return workspaceService.create(currentUserProvider.requireCurrentUserId(), request);
    }

    @GetMapping
    public List<WorkspaceResponse> listMine() {
        return workspaceService.listForUser(currentUserProvider.requireCurrentUserId());
    }

    @GetMapping("/{id}")
    public WorkspaceResponse getById(@PathVariable UUID id) {
        return workspaceService.getById(id, currentUserProvider.requireCurrentUserId());
    }

    @PatchMapping("/{id}")
    public WorkspaceResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateWorkspaceRequest request) {
        return workspaceService.update(id, currentUserProvider.requireCurrentUserId(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        workspaceService.delete(id, currentUserProvider.requireCurrentUserId());
    }
}
