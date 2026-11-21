package com.dps.layanan_pengajuan_cuti;

import com.dps.layanan_pengajuan_cuti.domain.LeaveApplication;
import com.dps.layanan_pengajuan_cuti.domain.LeaveApplicationRepository;
import com.dps.layanan_pengajuan_cuti.security.Role;
import com.dps.layanan_pengajuan_cuti.security.UserAccount;
import com.dps.layanan_pengajuan_cuti.security.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationAuthorizationTests {
	private static final String APPLICANT_EMAIL = "applicant@example.test";
	private static final String PASSWORD = "temporary-password-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private LeaveApplicationRepository leaveApplicationRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		leaveApplicationRepository.deleteAll();
		userAccountRepository.deleteAll();
		userAccountRepository.save(new UserAccount(
				APPLICANT_EMAIL, passwordEncoder.encode(PASSWORD), "Pemohon Satu", "NIK-1", Role.APPLICANT));
		userAccountRepository.save(new UserAccount(
				"hr@example.test", passwordEncoder.encode(PASSWORD), "HR Satu", "NIK-HR", Role.HR));

		leaveApplicationRepository.save(new LeaveApplication(
				UUID.randomUUID(), "Pemohon Satu", "NIK-1", APPLICANT_EMAIL,
				LocalDate.of(2026, 12, 10), "Pengajuan milik pemohon"));
		leaveApplicationRepository.save(new LeaveApplication(
				UUID.randomUUID(), "Pemohon Dua", "NIK-2", "other@example.test",
				LocalDate.of(2026, 12, 11), "Pengajuan milik orang lain"));
	}

	@Test
	void applicantCanOnlyListTheirOwnApplicationsAndCannotManageUsers() throws Exception {
		String token = login(APPLICANT_EMAIL);

		mockMvc.perform(get("/api/pengajuan-cuti").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].name").value("Pemohon Satu"));

		mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());
	}

	@Test
	void hrCanManageUsersAndUnauthenticatedRequestsAreRejected() throws Exception {
		mockMvc.perform(get("/api/pengajuan-cuti"))
				.andExpect(status().isUnauthorized());

		String token = login("hr@example.test");
		mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));
	}

	private String login(String email) throws Exception {
		String body = "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}";
		String response = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return response.replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");
	}
}
