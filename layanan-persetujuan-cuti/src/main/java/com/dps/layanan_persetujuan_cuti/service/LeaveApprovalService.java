package com.dps.layanan_persetujuan_cuti.service;

import com.dps.layanan_persetujuan_cuti.api.LeaveApprovalResponse;
import com.dps.layanan_persetujuan_cuti.api.LeaveDecisionRequest;
import com.dps.layanan_persetujuan_cuti.api.LeaveRequestedEvent;
import com.dps.layanan_persetujuan_cuti.domain.LeaveApproval;
import com.dps.layanan_persetujuan_cuti.domain.LeaveApprovalRepository;
import com.dps.layanan_persetujuan_cuti.domain.LeaveStatus;
import com.dps.layanan_persetujuan_cuti.messaging.KafkaConfiguration;
import com.dps.layanan_persetujuan_cuti.messaging.LeaveDecisionEvent;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class LeaveApprovalService {
	private final LeaveApprovalRepository repository;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	public LeaveApprovalService(
			LeaveApprovalRepository repository,
			KafkaTemplate<String, Object> kafkaTemplate) {
		this.repository = repository;
		this.kafkaTemplate = kafkaTemplate;
	}

	@Transactional
	public void receive(LeaveRequestedEvent event) {
		if (repository.existsById(event.requestId())) {
			return;
		}
		repository.save(new LeaveApproval(
				event.requestId(), event.name(), event.nik(), event.leaveDate(), event.reason()));
	}

	@Transactional(readOnly = true)
	public List<LeaveApprovalResponse> getAll(LeaveStatus status) {
		List<LeaveApproval> approvals = status == null
				? repository.findAll(Sort.by(Sort.Direction.ASC, "createdAt"))
				: repository.findAllByStatusOrderByCreatedAtAsc(status);
		return approvals.stream().map(LeaveApprovalService::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public LeaveApprovalResponse getById(UUID requestId) {
		return repository.findById(requestId)
				.map(LeaveApprovalService::toResponse)
				.orElseThrow(() -> new EntityNotFoundException("Pengajuan cuti tidak ditemukan."));
	}

	@Transactional
	public LeaveApprovalResponse decide(UUID requestId, LeaveDecisionRequest request) {
		if (request.status() == LeaveStatus.PENDING) {
			throw new IllegalArgumentException("Keputusan harus APPROVED atau REJECTED.");
		}

		LeaveApproval approval = repository.findByIdForUpdate(requestId)
				.orElseThrow(() -> new EntityNotFoundException("Pengajuan cuti tidak ditemukan."));
		if (approval.getStatus() != LeaveStatus.PENDING) {
			throw new IllegalStateException("Pengajuan cuti sudah diputuskan.");
		}

		String decisionNote = request.decisionNote() == null ? null : request.decisionNote().trim();
		approval.applyDecision(request.status(), decisionNote);
		repository.flush();
		LeaveDecisionEvent event = new LeaveDecisionEvent(
				requestId, request.status(), decisionNote, OffsetDateTime.now(ZoneOffset.UTC));
		try {
			kafkaTemplate.send(KafkaConfiguration.DECISIONS_TOPIC, requestId.toString(), event)
					.get(10, TimeUnit.SECONDS);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new MessagingException("Pengiriman keputusan ke layanan pengajuan terganggu.", exception);
		} catch (Exception exception) {
			throw new MessagingException("Keputusan tidak dapat dikirim ke layanan pengajuan.", exception);
		}

		return toResponse(approval);
	}

	private static LeaveApprovalResponse toResponse(LeaveApproval approval) {
		return new LeaveApprovalResponse(
				approval.getRequestId(),
				approval.getName(),
				approval.getNik(),
				approval.getLeaveDate(),
				approval.getReason(),
				approval.getStatus(),
				approval.getDecisionNote(),
				approval.getCreatedAt(),
				approval.getUpdatedAt());
	}
}
