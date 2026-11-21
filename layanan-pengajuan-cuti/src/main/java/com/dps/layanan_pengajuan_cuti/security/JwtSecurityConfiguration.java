package com.dps.layanan_pengajuan_cuti.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class JwtSecurityConfiguration {
	@Bean
	SecretKey jwtSecretKey(@Value("${app.security.jwt-secret-base64}") String encodedSecret) {
		byte[] secret = Base64.getDecoder().decode(encodedSecret);
		if (secret.length < 32) {
			throw new IllegalStateException("JWT_SECRET_BASE64 harus berisi minimal 32 byte acak.");
		}
		return new SecretKeySpec(secret, "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(TokenService.ISSUER));
		return decoder;
	}

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
		authoritiesConverter.setAuthoritiesClaimName("roles");
		authoritiesConverter.setAuthorityPrefix("");
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		return converter;
	}

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
						.requestMatchers("/api/auth/me", "/api/auth/change-password").authenticated()
						.requestMatchers("/api/admin/**").hasRole("HR")
						.requestMatchers(HttpMethod.POST, "/api/pengajuan-cuti").hasRole("APPLICANT")
						.requestMatchers(HttpMethod.GET, "/api/pengajuan-cuti", "/api/pengajuan-cuti/**")
								.hasAnyRole("APPLICANT", "OFFICER", "HR")
								.anyRequest().denyAll())
						.oauth2ResourceServer(resourceServer -> resourceServer
								.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
						.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
