package com.dps.layanan_persetujuan_cuti;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApprovalAuthorizationTests {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void approvalApiRequiresAnAuthenticatedUser() throws Exception {
		mockMvc.perform(get("/api/persetujuan-cuti"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void applicantsCannotAccessApprovalQueue() throws Exception {
		mockMvc.perform(get("/api/persetujuan-cuti")
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_APPLICANT"))))
				.andExpect(status().isForbidden());
	}

	@Test
	void officersCanAccessApprovalQueue() throws Exception {
		mockMvc.perform(get("/api/persetujuan-cuti")
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_OFFICER"))))
				.andExpect(status().isOk());
	}
}
