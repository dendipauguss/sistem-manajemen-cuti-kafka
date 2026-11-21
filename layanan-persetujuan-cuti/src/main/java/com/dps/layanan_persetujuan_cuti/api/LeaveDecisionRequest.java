package com.dps.layanan_persetujuan_cuti.api;

import com.dps.layanan_persetujuan_cuti.domain.LeaveStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LeaveDecisionRequest(
		@NotNull LeaveStatus status,
		@Size(max = 1000) String decisionNote) {
}
