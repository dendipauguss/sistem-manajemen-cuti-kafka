package com.dps.layanan_pengajuan_cuti.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
		@NotBlank @Email String email,
		@NotBlank String password) {
}
