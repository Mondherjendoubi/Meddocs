package com.meddocs.auth;

import com.meddocs.auth.dto.AuthResponse;
import com.meddocs.auth.dto.LoginRequest;
import com.meddocs.auth.dto.RegisterRequest;
import com.meddocs.auth.dto.UserResponse;
import com.meddocs.model.User;
import com.meddocs.repository.UserRepository;
import com.meddocs.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registration and login. Holds no HTTP concerns — the controller maps results to responses. */
@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	/** Create a new user with a BCrypt-hashed password. */
	@Transactional
	public UserResponse register(RegisterRequest request) {
		String email = request.email().toLowerCase().trim();
		if (userRepository.existsByEmail(email)) {
			throw new EmailAlreadyUsedException(email);
		}
		User user = new User(email, passwordEncoder.encode(request.password()));
		return UserResponse.from(userRepository.save(user));
	}

	/**
	 * Verify credentials via the {@link AuthenticationManager} (which checks the BCrypt
	 * hash) and, on success, issue a JWT. Bad credentials propagate as
	 * {@code BadCredentialsException} → HTTP 401.
	 */
	public AuthResponse login(LoginRequest request) {
		String email = request.email().toLowerCase().trim();
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(email, request.password()));
		return AuthResponse.bearer(jwtService.generateToken(authentication.getName()));
	}
}
