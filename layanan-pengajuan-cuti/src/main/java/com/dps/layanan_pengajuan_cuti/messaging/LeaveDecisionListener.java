package com.dps.layanan_pengajuan_cuti.messaging;

import com.dps.layanan_pengajuan_cuti.service.LeaveApplicationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class LeaveDecisionListener {
	private final LeaveApplicationService leaveApplicationService;

	public LeaveDecisionListener(LeaveApplicationService leaveApplicationService) {
		this.leaveApplicationService = leaveApplicationService;
	}

	@KafkaListener(
			topics = KafkaConfiguration.DECISIONS_TOPIC,
			containerFactory = "leaveDecisionKafkaListenerContainerFactory")
	public void receive(LeaveDecisionEvent event) {
		leaveApplicationService.applyDecision(event.requestId(), event.status(), event.decisionNote());
	}
}
