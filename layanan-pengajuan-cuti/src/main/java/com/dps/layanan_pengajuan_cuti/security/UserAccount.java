package com.dps.layanan_pengajuan_cuti.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "user_accounts")
public class UserAccount {
	@Id
	private UUID id;

	@Column(nullable = false, unique = true, length = 254)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(nullable = false, length = 30)
	private String nik;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	protected UserAccount() {
	}

	public UserAccount(String email, String passwordHash, String name, String nik, Role role) {
		this.id = UUID.randomUUID();
		this.email = email;
		this.passwordHash = passwordHash;
		this.name = name;
		this.nik = nik;
		this.role = role;
	}

	@PrePersist
	void onCreate() {
		createdAt = OffsetDateTime.now(ZoneOffset.UTC);
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getName() {
		return name;
	}

	public String getNik() {
		return nik;
	}

	public Role getRole() {
		return role;
	}

	public void changePasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}
}
