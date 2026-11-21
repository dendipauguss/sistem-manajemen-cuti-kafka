package com.dps.layanan_pengajuan_cuti.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateLeaveRequest(
		@NotBlank @Size(max = 150) String name,
		@NotBlank @Size(max = 30) String nik,
		@NotNull LocalDate leaveDate,
		@NotBlank @Size(max = 1000) String reason) {
}
