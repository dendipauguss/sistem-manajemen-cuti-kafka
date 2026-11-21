package com.dps.layanan_persetujuan_cuti.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveApprovalRepository extends JpaRepository<LeaveApproval, UUID> {
	List<LeaveApproval> findAllByStatusOrderByCreatedAtAsc(LeaveStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select approval from LeaveApproval approval where approval.requestId = :requestId")
	Optional<LeaveApproval> findByIdForUpdate(@Param("requestId") UUID requestId);
}
