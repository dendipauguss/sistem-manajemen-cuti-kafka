package com.dps.layanan_pengajuan_cuti.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, UUID> {
	List<LeaveApplication> findAllBySubmittedByEmailOrderByCreatedAtDesc(String submittedByEmail);
}
