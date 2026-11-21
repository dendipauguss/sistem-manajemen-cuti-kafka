package com.dps.layanan_pengajuan_cuti.security;

public record SessionUser(String email, String name, String nik, Role role) {
}
