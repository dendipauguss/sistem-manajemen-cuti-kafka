package com.dps.layanan_pengajuan_cuti.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "leave_applications")
public class LeaveApplication {
	@Id
	private UUID id;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(nullable = false, length = 30)
	private String nik;

	@Column(name = "submitted_by_email", length = 254)
	private String submittedByEmail;

	@Column(name = "leave_date", nullable = false)
	private LocalDate leaveDate;

	@Column(nullable = false, length = 1000)
	private String reason;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private LeaveStatus status;

	@Column(name = "decision_note", length = 1000)
	private String decisionNote;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	protected LeaveApplication() {
	}

	public LeaveApplication(
			UUID id, String name, String nik, String submittedByEmail, LocalDate leaveDate, String reason) {
		this.id = id;
		this.name = name;
		this.nik = nik;
		this.submittedByEmail = submittedByEmail;
		this.leaveDate = leaveDate;
		this.reason = reason;
		this.status = LeaveStatus.PENDING;
	}

	@PrePersist
	void onCreate() {
		createdAt = OffsetDateTime.now(ZoneOffset.UTC);
		updatedAt = createdAt;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getNik() {
		return nik;
	}

	public String getSubmittedByEmail() {
		return submittedByEmail;
	}

	public LocalDate getLeaveDate() {
		return leaveDate;
	}

	public String getReason() {
		return reason;
	}

	public LeaveStatus getStatus() {
		return status;
	}

	public String getDecisionNote() {
		return decisionNote;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void applyDecision(LeaveStatus status, String decisionNote) {
		this.status = status;
		this.decisionNote = decisionNote;
	}
}
