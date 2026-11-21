package com.dps.layanan_pengajuan_cuti.security;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserProfile user) {
}
