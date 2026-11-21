package com.dps.layanan_pengajuan_cuti.service;

import com.dps.layanan_pengajuan_cuti.api.CreateLeaveRequest;
import com.dps.layanan_pengajuan_cuti.api.LeaveResponse;
import com.dps.layanan_pengajuan_cuti.domain.LeaveApplication;
import com.dps.layanan_pengajuan_cuti.domain.LeaveApplicationRepository;
import com.dps.layanan_pengajuan_cuti.domain.LeaveStatus;
import com.dps.layanan_pengajuan_cuti.messaging.KafkaConfiguration;
import com.dps.layanan_pengajuan_cuti.messaging.LeaveRequestedEvent;
import com.dps.layanan_pengajuan_cuti.security.Role;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class LeaveApplicationService {
	private final LeaveApplicationRepository repository;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	public LeaveApplicationService(
			LeaveApplicationRepository repository,
			KafkaTemplate<String, Object> kafkaTemplate) {
		this.repository = repository;
		this.kafkaTemplate = kafkaTemplate;
	}

	@Transactional
	public LeaveResponse create(CreateLeaveRequest request, String submittedByEmail, String name, String nik) {
		UUID id = UUID.randomUUID();
		LeaveApplication application = repository.saveAndFlush(new LeaveApplication(
				id, name, nik, submittedByEmail, request.leaveDate(), request.reason().trim()));
		LeaveRequestedEvent event = new LeaveRequestedEvent(
				id, application.getName(), application.getNik(), application.getLeaveDate(), application.getReason());

		try {
			kafkaTemplate.send(KafkaConfiguration.REQUESTS_TOPIC, id.toString(), event)
					.get(10, TimeUnit.SECONDS);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new MessagingException("Pengiriman pengajuan ke layanan persetujuan terganggu.", exception);
		} catch (Exception exception) {
			throw new MessagingException("Pengajuan tersimpan tetapi gagal dikirim ke layanan persetujuan.", exception);
		}

		return toResponse(application);
	}

	@Transactional(readOnly = true)
	public List<LeaveResponse> getAll(String userEmail, Role role) {
		List<LeaveApplication> applications = role == Role.APPLICANT
				? repository.findAllBySubmittedByEmailOrderByCreatedAtDesc(userEmail)
				: repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
		return applications
				.stream()
				.map(LeaveApplicationService::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public LeaveResponse getById(UUID id, String userEmail, Role role) {
		return repository.findById(id)
				.filter(application -> role != Role.APPLICANT
						|| userEmail.equals(application.getSubmittedByEmail()))
				.map(LeaveApplicationService::toResponse)
				.orElseThrow(() -> new EntityNotFoundException("Pengajuan cuti tidak ditemukan."));
	}

	@Transactional
	public void applyDecision(UUID requestId, LeaveStatus status, String decisionNote) {
		LeaveApplication application = repository.findById(requestId)
				.orElseThrow(() -> new EntityNotFoundException(
						"Pengajuan cuti untuk keputusan tidak ditemukan: " + requestId));
		application.applyDecision(status, decisionNote);
	}

	private static LeaveResponse toResponse(LeaveApplication application) {
		return new LeaveResponse(
				application.getId(),
				application.getName(),
				application.getNik(),
				application.getLeaveDate(),
				application.getReason(),
				application.getStatus(),
				application.getDecisionNote(),
				application.getCreatedAt(),
				application.getUpdatedAt());
	}
}
