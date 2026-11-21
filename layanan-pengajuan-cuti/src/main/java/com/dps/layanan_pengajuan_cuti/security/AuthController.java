package com.dps.layanan_pengajuan_cuti.security;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final AuthenticationService authenticationService;

	public AuthController(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return authenticationService.login(request);
	}

	@PostMapping("/change-password")
	public void changePassword(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody ChangePasswordRequest request) {
		authenticationService.changePassword(jwt.getSubject(), request);
	}

	@GetMapping("/me")
	public SessionUser me(@AuthenticationPrincipal Jwt jwt) {
		return new SessionUser(jwt.getSubject(), jwt.getClaimAsString("name"),
				jwt.getClaimAsString("nik"), Role.valueOf(jwt.getClaimAsStringList("roles").getFirst()
						.replace("ROLE_", "")));
	}
}
