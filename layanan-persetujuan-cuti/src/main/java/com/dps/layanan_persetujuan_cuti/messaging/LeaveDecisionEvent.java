package com.dps.layanan_persetujuan_cuti.messaging;

import com.dps.layanan_persetujuan_cuti.domain.LeaveStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LeaveDecisionEvent(UUID requestId, LeaveStatus status, String decisionNote, OffsetDateTime decidedAt) {
}
