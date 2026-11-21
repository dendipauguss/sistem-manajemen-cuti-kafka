package com.dps.layanan_pengajuan_cuti.security;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class TokenService {
	public static final String ISSUER = "sistem-manajemen-cuti";
	private static final long TOKEN_LIFETIME_MINUTES = 60;

	private final JwtEncoder jwtEncoder;

	public TokenService(JwtEncoder jwtEncoder) {
		this.jwtEncoder = jwtEncoder;
	}

	public String issue(UserAccount user) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(ISSUER)
				.issuedAt(now)
				.expiresAt(now.plus(TOKEN_LIFETIME_MINUTES, ChronoUnit.MINUTES))
				.subject(user.getEmail())
				.claim("name", user.getName())
				.claim("nik", user.getNik())
				.claim("roles", List.of("ROLE_" + user.getRole().name()))
				.build();
		return jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
	}
}
