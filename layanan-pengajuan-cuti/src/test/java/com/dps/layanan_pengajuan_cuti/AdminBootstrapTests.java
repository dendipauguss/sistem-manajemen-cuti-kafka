package com.dps.layanan_pengajuan_cuti;

import com.dps.layanan_pengajuan_cuti.security.Role;
import com.dps.layanan_pengajuan_cuti.security.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
		"app.bootstrap-admin.email=bootstrap@example.test",
		"app.bootstrap-admin.password=bootstrap-password-123",
		"app.bootstrap-admin.name=Bootstrap Administrator",
		"app.bootstrap-admin.nik=HR-TEST"
})
class AdminBootstrapTests {
	@Autowired
	private UserAccountRepository userAccountRepository;

	@Test
	void createsInitialHrAccountFromBootstrapProperties() {
		var account = userAccountRepository.findByEmail("bootstrap@example.test").orElseThrow();

		assertThat(account.getName()).isEqualTo("Bootstrap Administrator");
		assertThat(account.getNik()).isEqualTo("HR-TEST");
		assertThat(account.getRole()).isEqualTo(Role.HR);
		assertThat(account.getPasswordHash()).isNotEqualTo("bootstrap-password-123");
	}
}
