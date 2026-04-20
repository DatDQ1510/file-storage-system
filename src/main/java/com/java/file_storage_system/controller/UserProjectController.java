package com.java.file_storage_system.controller;

import com.java.file_storage_system.context.UserContext;
import com.java.file_storage_system.dto.project.member.AddProjectMemberRequest;
import com.java.file_storage_system.dto.project.member.ProjectMemberResponse;
import com.java.file_storage_system.payload.ApiResponse;
import com.java.file_storage_system.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/user-projects")
public class UserProjectController {

    private final ProjectService projectService;
    private final UserContext userContext;

    @PostMapping("/{projectId}/members")
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> createUserProjectMembership(
	    @PathVariable("projectId") String projectId,
	    @Valid @RequestBody AddProjectMemberRequest request,
	    HttpServletRequest httpServletRequest
    ) {
	ProjectMemberResponse response = projectService.addUserToProject(
		projectId,
		request,
		userContext.getId(),
		userContext.getRole(),
		userContext.getTenantId()
	);

	return ResponseEntity.status(HttpStatus.CREATED)
		.body(ApiResponse.success("Add project member successfully", response, httpServletRequest.getRequestURI()));
    }
}
