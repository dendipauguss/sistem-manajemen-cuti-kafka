package com.dps.layanan_persetujuan_cuti.api;

import java.time.LocalDate;
import java.util.UUID;

public record LeaveRequestedEvent(UUID requestId, String name, String nik, LocalDate leaveDate, String reason) {
}
