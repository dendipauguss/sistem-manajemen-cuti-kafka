package com.dps.layanan_pengajuan_cuti.security;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserProfile(UUID id, String email, String name, String nik, Role role, OffsetDateTime createdAt) {
	public static UserProfile from(UserAccount user) {
		return new UserProfile(user.getId(), user.getEmail(), user.getName(), user.getNik(), user.getRole(),
				user.getCreatedAt());
	}
}
