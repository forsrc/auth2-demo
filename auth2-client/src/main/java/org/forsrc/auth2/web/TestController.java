package org.forsrc.auth2.web;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
public class TestController {

	@Autowired
	private WebClient webClient;

	@Value("${my-auth2.auth2-resource-server.messages}")
	private String messagesBaseUri;

	@GetMapping("/test")
	public Principal test(Principal principal) {
		return principal;
	}

	@GetMapping("/test/messages")
	public String[] messages() {
		// @formatter:off
		String[] messages = this.webClient
				.get()
				.uri(this.messagesBaseUri)
				.attributes(clientRegistrationId("auth2-client-client-credentials"))
				.retrieve()
				.bodyToMono(String[].class)
				.block();
		// @formatter:on
		return messages;
	}

}
