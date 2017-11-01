package io.github.vpavic.oauth2.endsession;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.LogoutRequest;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientInformation;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.util.UriComponentsBuilder;

import io.github.vpavic.oauth2.client.ClientRepository;

@RequestMapping(path = EndSessionEndpoint.PATH_MAPPING)
public class EndSessionEndpoint {

	public static final String PATH_MAPPING = "/oauth2/logout";

	private static final String POST_LOGOUT_REDIRECT_URI_PARAMETER = "post_logout_redirect_uri";

	private static final String STATE_PARAMETER = "state";

	private static final String LOGOUT_PROMPT_VIEW_NAME = "oauth2/logout-prompt";

	private static final String LOGOUT_SUCCESS_VIEW_NAME = "oauth2/logout-success";

	private final Issuer issuer;

	private final ClientRepository clientRepository;

	private boolean frontChannelLogoutEnabled;

	public EndSessionEndpoint(Issuer issuer, ClientRepository clientRepository) {
		Objects.requireNonNull(issuer, "issuer must not be null");
		Objects.requireNonNull(clientRepository, "clientRepository must not be null");

		this.issuer = issuer;
		this.clientRepository = clientRepository;
	}

	public void setFrontChannelLogoutEnabled(boolean frontChannelLogoutEnabled) {
		this.frontChannelLogoutEnabled = frontChannelLogoutEnabled;
	}

	@GetMapping
	public String getLogoutPrompt(HTTPRequest httpRequest, Model model) throws ParseException {
		if (httpRequest.getQuery() != null) {
			LogoutRequest logoutRequest = LogoutRequest.parse(httpRequest.getQuery());
			model.addAttribute("redirectURI", logoutRequest.getPostLogoutRedirectionURI());
			model.addAttribute("state", logoutRequest.getState());
		}

		return LOGOUT_PROMPT_VIEW_NAME;
	}

	@PostMapping
	public String handleLogoutSuccess(WebRequest request, Model model) {
		String postLogoutRedirectUri = request.getParameter(POST_LOGOUT_REDIRECT_URI_PARAMETER);

		List<OIDCClientInformation> clients = this.clientRepository.findAll();

		if (StringUtils.hasText(postLogoutRedirectUri)) {
			// @formatter:off
			Set<String> postLogoutRedirectUris = clients.stream()
					.flatMap(client -> Optional.ofNullable(client.getOIDCMetadata().getPostLogoutRedirectionURIs())
							.orElse(Collections.emptySet()).stream())
					.filter(Objects::nonNull)
					.map(URI::toString)
					.collect(Collectors.toSet());
			// @formatter:on

			if (postLogoutRedirectUris.contains(postLogoutRedirectUri)) {
				String state = request.getParameter(STATE_PARAMETER);

				if (state != null) {
					// @formatter:off
					postLogoutRedirectUri = UriComponentsBuilder.fromHttpUrl(postLogoutRedirectUri)
							.queryParam("state", state)
							.toUriString();
					// @formatter:on
				}
			}
			else {
				postLogoutRedirectUri = resolveDefaultPostLogoutRedirectUri();
			}
		}
		else {
			postLogoutRedirectUri = resolveDefaultPostLogoutRedirectUri();
		}

		model.addAttribute("postLogoutRedirectUri", postLogoutRedirectUri);

		if (this.frontChannelLogoutEnabled) {
			String sessionId = request.getSessionId();

			// @formatter:off
			List<String> frontChannelLogoutUris = clients.stream()
					.map(client -> client.getOIDCMetadata().getFrontChannelLogoutURI())
					.filter(Objects::nonNull)
					.map(uri -> buildFrontChannelLogoutUri(uri, sessionId))
					.collect(Collectors.toList());
			// @formatter:on

			model.addAttribute("frontChannelLogoutUris", frontChannelLogoutUris);
		}

		return LOGOUT_SUCCESS_VIEW_NAME;
	}

	private String resolveDefaultPostLogoutRedirectUri() {
		// @formatter:off
		return UriComponentsBuilder.fromHttpUrl(this.issuer.getValue())
				.path("/login")
				.query("logout")
				.toUriString();
		// @formatter:on
	}

	private String buildFrontChannelLogoutUri(URI uri, String sessionId) {
		// @formatter:off
		return UriComponentsBuilder.fromUri(uri)
				.queryParam("iss", this.issuer)
				.queryParam("sid", sessionId)
				.toUriString();
		// @formatter:on
	}

}
