package com.dps.layanan_persetujuan_cuti.api;

import com.dps.layanan_persetujuan_cuti.domain.LeaveStatus;
import com.dps.layanan_persetujuan_cuti.service.LeaveApprovalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/persetujuan-cuti")
public class LeaveApprovalController {
	private final LeaveApprovalService leaveApprovalService;

	public LeaveApprovalController(LeaveApprovalService leaveApprovalService) {
		this.leaveApprovalService = leaveApprovalService;
	}

	@GetMapping
	public List<LeaveApprovalResponse> getAll(@RequestParam(required = false) LeaveStatus status) {
		return leaveApprovalService.getAll(status);
	}

	@GetMapping("/{requestId}")
	public LeaveApprovalResponse getById(@PathVariable UUID requestId) {
		return leaveApprovalService.getById(requestId);
	}

	@PostMapping("/{requestId}/keputusan")
	@ResponseStatus(HttpStatus.OK)
	public LeaveApprovalResponse decide(
			@PathVariable UUID requestId,
			@Valid @RequestBody LeaveDecisionRequest request) {
		return leaveApprovalService.decide(requestId, request);
	}
}
