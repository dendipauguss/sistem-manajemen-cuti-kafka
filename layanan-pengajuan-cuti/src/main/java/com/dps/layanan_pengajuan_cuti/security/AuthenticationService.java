package com.dps.layanan_pengajuan_cuti.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.nio.charset.StandardCharsets;

@Service
public class AuthenticationService {
	private final UserAccountRepository repository;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;

	public AuthenticationService(
			UserAccountRepository repository,
			PasswordEncoder passwordEncoder,
			TokenService tokenService) {
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
	}

	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request) {
		UserAccount user = repository.findByEmail(normalizeEmail(request.email()))
				.filter(account -> passwordEncoder.matches(request.password(), account.getPasswordHash()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
						"Email atau kata sandi tidak valid."));
		return new LoginResponse(tokenService.issue(user), "Bearer", 3600, UserProfile.from(user));
	}

	@Transactional
	public void changePassword(String email, ChangePasswordRequest request) {
		UserAccount user = repository.findByEmail(normalizeEmail(email))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
						"Sesi tidak valid. Silakan masuk kembali."));
		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kata sandi saat ini tidak sesuai.");
		}
		if (request.newPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new IllegalArgumentException("Kata sandi maksimal 72 byte UTF-8.");
		}
		user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
	}

	@Transactional(readOnly = true)
	public List<UserProfile> listUsers() {
		return repository.findAll().stream()
				.map(UserProfile::from)
				.toList();
	}

	@Transactional
	public UserProfile createUser(CreateUserRequest request) {
		String email = normalizeEmail(request.email());
		if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new IllegalArgumentException("Kata sandi maksimal 72 byte UTF-8.");
		}
		if (repository.existsByEmail(email)) {
			throw new IllegalStateException("Email tersebut sudah terdaftar.");
		}
		UserAccount user = repository.save(new UserAccount(
				email,
				passwordEncoder.encode(request.password()),
				request.name().trim(),
				request.nik().trim(),
				request.role()));
		return UserProfile.from(user);
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
