package com.dps.layanan_pengajuan_cuti.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
public class AdminBootstrap implements ApplicationRunner {
	private static final Logger logger = LoggerFactory.getLogger(AdminBootstrap.class);

	private final UserAccountRepository repository;
	private final PasswordEncoder passwordEncoder;
	private final String email;
	private final String password;
	private final String name;
	private final String nik;

	public AdminBootstrap(
			UserAccountRepository repository,
			PasswordEncoder passwordEncoder,
			@Value("${app.bootstrap-admin.email:}") String email,
			@Value("${app.bootstrap-admin.password:}") String password,
			@Value("${app.bootstrap-admin.name:}") String name,
			@Value("${app.bootstrap-admin.nik:}") String nik) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.email = email.trim().toLowerCase(Locale.ROOT);
		this.password = password;
		this.name = name.trim();
		this.nik = nik.trim();
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		boolean allEmpty = email.isEmpty() && password.isEmpty() && name.isEmpty() && nik.isEmpty();
		if (allEmpty) {
			logger.warn("HR bootstrap belum dikonfigurasi; tidak ada akun HR awal yang dibuat.");
			return;
		}
		if (email.isEmpty() || password.isBlank() || name.isEmpty() || nik.isEmpty()) {
			throw new IllegalStateException(
					"Konfigurasikan BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_PASSWORD, BOOTSTRAP_ADMIN_NAME, dan BOOTSTRAP_ADMIN_NIK.");
		}
		if (password.length() < 12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
			throw new IllegalStateException("Kata sandi bootstrap HR harus 12-72 byte UTF-8.");
		}

		repository.findByEmail(email).ifPresentOrElse(existing -> {
			if (existing.getRole() != Role.HR) {
				throw new IllegalStateException("Email bootstrap sudah digunakan akun dengan peran non-HR.");
			}
		}, () -> {
			repository.save(new UserAccount(email, passwordEncoder.encode(password), name, nik, Role.HR));
			logger.info("Akun HR bootstrap dibuat untuk {}", email);
		});
	}
}
