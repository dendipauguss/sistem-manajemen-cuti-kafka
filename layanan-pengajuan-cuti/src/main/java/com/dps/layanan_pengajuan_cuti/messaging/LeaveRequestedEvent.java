package com.dps.layanan_pengajuan_cuti.messaging;

import java.time.LocalDate;
import java.util.UUID;

public record LeaveRequestedEvent(UUID requestId, String name, String nik, LocalDate leaveDate, String reason) {
}
