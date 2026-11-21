package com.dps.layanan_pengajuan_cuti.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
		@NotBlank String currentPassword,
		@NotBlank @Size(min = 12, max = 72) String newPassword) {
}
