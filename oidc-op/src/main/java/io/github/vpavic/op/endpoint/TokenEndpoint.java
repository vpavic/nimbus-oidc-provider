package io.github.vpavic.op.endpoint;

import java.security.PublicKey;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWT;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.GeneralException;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.auth.verifier.ClientAuthenticationVerifier;
import com.nimbusds.oauth2.sdk.auth.verifier.ClientCredentialsSelector;
import com.nimbusds.oauth2.sdk.auth.verifier.Context;
import com.nimbusds.oauth2.sdk.auth.verifier.InvalidClientException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.pkce.CodeChallenge;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientInformation;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientMetadata;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import net.minidev.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.vpavic.op.client.ClientRepository;
import io.github.vpavic.op.code.AuthorizationCodeContext;
import io.github.vpavic.op.code.AuthorizationCodeService;
import io.github.vpavic.op.config.OpenIdProviderProperties;
import io.github.vpavic.op.token.TokenService;

@RestController
@RequestMapping(path = "/token")
public class TokenEndpoint {

	private final OpenIdProviderProperties properties;

	private final ClientRepository clientRepository;

	private final AuthorizationCodeService authorizationCodeService;

	private final TokenService tokenService;

	public TokenEndpoint(OpenIdProviderProperties properties, ClientRepository clientRepository,
			AuthorizationCodeService authorizationCodeService, TokenService tokenService) {
		this.properties = properties;
		this.clientRepository = Objects.requireNonNull(clientRepository);
		this.authorizationCodeService = Objects.requireNonNull(authorizationCodeService);
		this.tokenService = Objects.requireNonNull(tokenService);
	}

	@PostMapping
	public JSONObject handleTokenRequest(HTTPRequest request) throws Exception {
		TokenRequest tokenRequest = TokenRequest.parse(request);

		validateClient(tokenRequest);

		AuthorizationGrant authorizationGrant = tokenRequest.getAuthorizationGrant();

		// Authorization Code Grant Type
		if (authorizationGrant instanceof AuthorizationCodeGrant) {
			AuthorizationCodeGrant authorizationCodeGrant = (AuthorizationCodeGrant) authorizationGrant;
			AuthorizationCodeContext context = this.authorizationCodeService
					.consume(authorizationCodeGrant.getAuthorizationCode());

			if (context == null) {
				throw new GeneralException(OAuth2Error.INVALID_GRANT);
			}

			AuthorizationRequest authRequest = context.getAuthRequest();
			CodeChallenge codeChallenge = authRequest.getCodeChallenge();

			if (codeChallenge != null) {
				CodeChallengeMethod codeChallengeMethod = authRequest.getCodeChallengeMethod();

				if (codeChallengeMethod == null) {
					codeChallengeMethod = CodeChallengeMethod.PLAIN;
				}

				CodeVerifier codeVerifier = authorizationCodeGrant.getCodeVerifier();

				if (codeVerifier == null
						|| !codeChallenge.equals(CodeChallenge.compute(codeChallengeMethod, codeVerifier))) {
					throw new GeneralException(OAuth2Error.INVALID_REQUEST);
				}
			}

			Authentication authentication = context.getAuthentication();
			UserDetails principal = (UserDetails) authentication.getPrincipal();

			AccessToken accessToken = this.tokenService.createAccessToken(authRequest, principal);
			RefreshToken refreshToken = this.tokenService.createRefreshToken(authRequest, principal);

			AccessTokenResponse tokenResponse;

			if (authRequest instanceof AuthenticationRequest) {
				JWT idToken = this.tokenService.createIdToken((AuthenticationRequest) authRequest, principal);
				OIDCTokens tokens = new OIDCTokens(idToken.serialize(), accessToken, refreshToken);

				tokenResponse = new OIDCTokenResponse(tokens);
			}
			else {
				Tokens tokens = new Tokens(accessToken, refreshToken);

				tokenResponse = new AccessTokenResponse(tokens);
			}

			return tokenResponse.toJSONObject();
		}

		return new TokenErrorResponse(OAuth2Error.UNSUPPORTED_GRANT_TYPE).toJSONObject();
	}

	private void validateClient(TokenRequest tokenRequest) throws Exception {
		ClientAuthentication clientAuthentication = tokenRequest.getClientAuthentication();
		ClientID clientID = (clientAuthentication != null) ? clientAuthentication.getClientID()
				: tokenRequest.getClientID();

		if (clientID == null) {
			throw InvalidClientException.BAD_ID;
		}

		OIDCClientInformation client = this.clientRepository.findByClientId(clientID);

		if (client == null) {
			throw InvalidClientException.BAD_ID;
		}

		OIDCClientMetadata clientMetadata = client.getOIDCMetadata();
		ClientAuthenticationMethod authMethod = clientMetadata.getTokenEndpointAuthMethod();

		if (clientAuthentication != null) {
			if (!authMethod.equals(clientAuthentication.getMethod())) {
				throw InvalidClientException.NOT_REGISTERED_FOR_AUTH_METHOD;
			}

			ClientAuthenticationVerifier<Void> verifier = new ClientAuthenticationVerifier<>(
					new ClientInformationCredentialsSelector(client), null,
					Collections.singleton(new Audience(this.properties.getIssuer())));
			verifier.verify(clientAuthentication, null, null);
		}
		else {
			if (!authMethod.equals(ClientAuthenticationMethod.NONE)) {
				throw InvalidClientException.NOT_REGISTERED_FOR_AUTH_METHOD;
			}
		}
	}

	@ExceptionHandler(GeneralException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public JSONObject handleParseException(GeneralException e) {
		ErrorObject error = e.getErrorObject();

		if (error == null) {
			error = OAuth2Error.INVALID_REQUEST.setDescription(e.getMessage());
		}

		return new TokenErrorResponse(error).toJSONObject();
	}

	private static class ClientInformationCredentialsSelector implements ClientCredentialsSelector<Void> {

		private final OIDCClientInformation clientInformation;

		private ClientInformationCredentialsSelector(OIDCClientInformation clientInformation) {
			this.clientInformation = Objects.requireNonNull(clientInformation);
		}

		@Override
		public List<Secret> selectClientSecrets(ClientID claimedClientID, ClientAuthenticationMethod authMethod,
				Context<Void> context) throws InvalidClientException {
			if (!claimedClientID.equals(this.clientInformation.getID())
					|| !authMethod.equals(this.clientInformation.getOIDCMetadata().getTokenEndpointAuthMethod())) {
				return Collections.emptyList();
			}
			return Collections.singletonList(this.clientInformation.getSecret());
		}

		@Override
		public List<? extends PublicKey> selectPublicKeys(ClientID claimedClientID,
				ClientAuthenticationMethod authMethod, JWSHeader jwsHeader, boolean forceRefresh, Context<Void> context)
				throws InvalidClientException {
			return Collections.emptyList();
		}

	}

}
