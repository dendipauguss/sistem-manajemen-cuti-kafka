package com.dps.layanan_persetujuan_cuti.domain;

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
@Table(name = "leave_approvals")
public class LeaveApproval {
	@Id
	private UUID requestId;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(nullable = false, length = 30)
	private String nik;

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

	protected LeaveApproval() {
	}

	public LeaveApproval(UUID requestId, String name, String nik, LocalDate leaveDate, String reason) {
		this.requestId = requestId;
		this.name = name;
		this.nik = nik;
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

	public UUID getRequestId() {
		return requestId;
	}

	public String getName() {
		return name;
	}

	public String getNik() {
		return nik;
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
