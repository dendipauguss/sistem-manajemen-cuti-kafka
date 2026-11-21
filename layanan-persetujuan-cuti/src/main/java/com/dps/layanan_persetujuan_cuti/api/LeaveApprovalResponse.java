package com.dps.layanan_persetujuan_cuti.api;

import com.dps.layanan_persetujuan_cuti.domain.LeaveStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LeaveApprovalResponse(
		UUID requestId,
		String name,
		String nik,
		LocalDate leaveDate,
		String reason,
		LeaveStatus status,
		String decisionNote,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {
}
