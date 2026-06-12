package com.meddocs.security;

import com.meddocs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Bridges our {@link com.meddocs.model.User} entity to Spring Security's
 * {@link UserDetails}. Used only at <em>login</em> time, by the
 * {@code AuthenticationManager}, to look up the stored BCrypt hash for verification.
 * (Per-request authentication goes through {@link JwtAuthenticationFilter} and does
 * not touch this.)
 */
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		return userRepository.findByEmail(email)
				.map(user -> User.withUsername(user.getEmail())
						.password(user.getPasswordHash())
						.authorities(List.of())
						.build())
				.orElseThrow(() -> new UsernameNotFoundException("No user with email " + email));
	}
}
