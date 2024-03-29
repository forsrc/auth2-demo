package org.forsrc.auth2.config;

import static org.springframework.security.config.Customizer.withDefaults;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.forsrc.auth2.entity.User;
import org.forsrc.auth2.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@EnableWebSecurity
@Configuration
public class DefaultSecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public UserDetailsService userDetailsService() {
		return new MyUserDetailsService();
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(userDetailsService());
		authProvider.setPasswordEncoder(passwordEncoder());

		return authProvider;
	}

	// @formatter:off
	@Bean
	SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> {
                    	authorize.requestMatchers("/actuator/**", "/oauth/logout").permitAll();
                    	authorize.anyRequest().authenticated();
                })
                .formLogin(withDefaults())
                .logout(logout -> {
                	logout.clearAuthentication(true)
            		.invalidateHttpSession(true)
            		.deleteCookies("JSESSIONID")
            		.logoutSuccessUrl("/")
            		.logoutUrl("/logout**")
            		.logoutSuccessHandler(logoutSuccessHandler())
            		.permitAll();
                })
                .authenticationProvider(authenticationProvider())
                .userDetailsService(userDetailsService())
			;
		return http.build();
	}
	// @formatter:on
	
	private LogoutSuccessHandler logoutSuccessHandler() {
		return new LogoutSuccessHandler() {

			@Override
			public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
					Authentication authentication) throws IOException, ServletException {
				final Authentication auth = SecurityContextHolder.getContext().getAuthentication();

				if (auth == null) {
					request.getRequestDispatcher("/?logout").forward(request, response);
					return;
				}

				new SecurityContextLogoutHandler().logout(request, response, auth);
				auth.setAuthenticated(false);
				SecurityContextHolder.clearContext();
				for (Cookie cookie : request.getCookies()) {
					String cookieName = cookie.getName();
					Cookie cookieToDelete = new Cookie(cookieName, null);
					cookieToDelete.setPath(request.getContextPath() + "/");
					cookieToDelete.setMaxAge(0);
					response.addCookie(cookieToDelete);
				}
				SecurityContextHolder.getContext().setAuthentication(null);
				request.getRequestDispatcher("/?logout").forward(request, response);


			}

		};
	}

	// @formatter:off
//	@Bean
//	UserDetailsService users() {
//		UserDetails user = User.withDefaultPasswordEncoder()
//				.username("forsrc")
//				.password("forsrc")
//				.roles("USER")
//				.build();
//		return new InMemoryUserDetailsManager(user);
//	}
	// @formatter:on

	public static class MyUserDetailsService implements UserDetailsService {

		protected static final Logger LOGGER = LoggerFactory.getLogger(MyUserDetailsService.class);

		@Autowired
		private UserService userService;

		@Override
		public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
			User user = userService.findByUsername(username);
			LOGGER.info("--> MyUserDetailsService.loadUserByUsername() --> User is : {} ({})", username, user);
			if (user != null) {
				org.springframework.security.core.userdetails.User u = new org.springframework.security.core.userdetails.User(
						user.getUsername(), user.getPassword(), getAuthorities(user));
				return u;

			}

			throw new UsernameNotFoundException("Could not find user: " + username);
		}

		public Collection<? extends GrantedAuthority> getAuthorities(User user) {

			List<String> roleList = userService.findRoleByUserId(user.getId());
			if (roleList == null || roleList.isEmpty()) {
				return new ArrayList<>();
			}

			List<GrantedAuthority> auths = new ArrayList<>();
			for (String name : roleList) {
				auths.add(new SimpleGrantedAuthority(name));
			}
			return auths;
		}

	}

}