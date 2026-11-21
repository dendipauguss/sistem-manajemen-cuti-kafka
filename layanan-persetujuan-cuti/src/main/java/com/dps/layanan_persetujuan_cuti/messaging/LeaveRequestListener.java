package com.dps.layanan_persetujuan_cuti.messaging;

import com.dps.layanan_persetujuan_cuti.api.LeaveRequestedEvent;
import com.dps.layanan_persetujuan_cuti.service.LeaveApprovalService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class LeaveRequestListener {
	private final LeaveApprovalService leaveApprovalService;

	public LeaveRequestListener(LeaveApprovalService leaveApprovalService) {
		this.leaveApprovalService = leaveApprovalService;
	}

	@KafkaListener(
			topics = KafkaConfiguration.REQUESTS_TOPIC,
			containerFactory = "leaveRequestKafkaListenerContainerFactory")
	public void receive(LeaveRequestedEvent event) {
		leaveApprovalService.receive(event);
	}
}
