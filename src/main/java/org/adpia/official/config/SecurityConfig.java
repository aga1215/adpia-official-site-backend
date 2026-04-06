package org.adpia.official.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.adpia.official.security.MemberDetailsService;
import org.adpia.official.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

	private final MemberDetailsService memberDetailsService;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(csrf -> csrf.disable())
			.cors(cors -> cors.configurationSource(corsConfig()))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

				.requestMatchers("/health").permitAll()
				.requestMatchers("/api/email/**").permitAll()
				.requestMatchers("/api/members/signup").permitAll()
				.requestMatchers("/api/members/login").permitAll()
				.requestMatchers("/api/members/refresh").permitAll()
				.requestMatchers("/api/members/password/reset/send-code").permitAll()
				.requestMatchers("/api/members/password/reset/verify-code").permitAll()
				.requestMatchers("/api/members/password/reset/confirm").permitAll()

				.requestMatchers(HttpMethod.GET, "/api/recruit/**").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/recruit/QA/**").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/recruit/NOTICE/**").authenticated()

				.requestMatchers(HttpMethod.GET, "/api/news/**").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/news/**").hasAnyRole("SUPER_ADMIN", "PRESIDENT")
				.requestMatchers(HttpMethod.PATCH, "/api/news/**").hasAnyRole("SUPER_ADMIN", "PRESIDENT")
				.requestMatchers(HttpMethod.DELETE, "/api/news/**").hasAnyRole("SUPER_ADMIN", "PRESIDENT")

				.requestMatchers(HttpMethod.PATCH, "/api/recruit/posts/**").permitAll()
				.requestMatchers(HttpMethod.DELETE, "/api/recruit/posts/**").permitAll()
				.requestMatchers(HttpMethod.PATCH, "/api/recruit/posts/*/pin").authenticated()

				.requestMatchers(HttpMethod.GET, "/api/recruit/posts/*/comments").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/recruit/posts/*/comments").permitAll()
				.requestMatchers(HttpMethod.DELETE, "/api/recruit/comments/*").permitAll()

				.requestMatchers(org.springframework.http.HttpMethod.POST, "/api/recruit/posts/*/publish").permitAll()


				.requestMatchers(HttpMethod.GET, "/api/link/**").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/files/presign").permitAll()

				.requestMatchers(HttpMethod.GET, "/api/executives/**").permitAll()

				.requestMatchers(HttpMethod.GET, "/api/popups/**").permitAll()

				.requestMatchers(HttpMethod.GET, "/api/history/**").permitAll()

				.requestMatchers(HttpMethod.GET, "/api/bylaw/**").permitAll()

				.requestMatchers("/api/posts/category/**").permitAll()
				.requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")

				.anyRequest().authenticated()
			)
			.authenticationProvider(authenticationProvider())
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(memberDetailsService);
		provider.setPasswordEncoder(passwordEncoder());
		return provider;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	public CorsConfigurationSource corsConfig() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOriginPatterns(List.of(
			"http://localhost:*",
			"http://127.0.0.1:*",
			"https://adpia.or.kr",
			"https://www.adpia.or.kr"
		));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);
		config.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
