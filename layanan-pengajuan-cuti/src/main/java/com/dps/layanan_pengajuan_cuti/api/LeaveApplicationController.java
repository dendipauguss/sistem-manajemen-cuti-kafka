package com.dps.layanan_pengajuan_cuti.api;

import com.dps.layanan_pengajuan_cuti.service.LeaveApplicationService;
import com.dps.layanan_pengajuan_cuti.security.Role;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pengajuan-cuti")
public class LeaveApplicationController {
	private final LeaveApplicationService leaveApplicationService;

	public LeaveApplicationController(LeaveApplicationService leaveApplicationService) {
		this.leaveApplicationService = leaveApplicationService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public LeaveResponse create(@Valid @RequestBody CreateLeaveRequest request, @AuthenticationPrincipal Jwt jwt) {
		return leaveApplicationService.create(request, jwt.getSubject(),
				jwt.getClaimAsString("name"), jwt.getClaimAsString("nik"));
	}

	@GetMapping
	public List<LeaveResponse> getAll(@AuthenticationPrincipal Jwt jwt) {
		return leaveApplicationService.getAll(jwt.getSubject(), role(jwt));
	}

	@GetMapping("/{id}")
	public LeaveResponse getById(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
		return leaveApplicationService.getById(id, jwt.getSubject(), role(jwt));
	}

	private static Role role(Jwt jwt) {
		return Role.valueOf(jwt.getClaimAsStringList("roles").getFirst().replace("ROLE_", ""));
	}
}
