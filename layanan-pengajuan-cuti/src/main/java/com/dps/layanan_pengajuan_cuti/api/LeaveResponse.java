package com.dps.layanan_pengajuan_cuti.api;

import com.dps.layanan_pengajuan_cuti.domain.LeaveStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LeaveResponse(
		UUID id,
		String name,
		String nik,
		LocalDate leaveDate,
		String reason,
		LeaveStatus status,
		String decisionNote,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {
}
