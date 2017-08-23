package io.github.vpavic.op.config;

import java.time.Duration;
import java.time.Period;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("oidc.op")
public class OpenIdProviderProperties {

	private String issuer = "http://localhost:6432";

	private Duration accessTokenValidityDuration = Duration.ofMinutes(30);

	private Duration refreshTokenValidityDuration = Duration.ofDays(30);

	private Duration idTokenValidityDuration = Duration.ofMinutes(30);

	private Period jwkRetentionPeriod = Period.ofDays(10);

	private boolean sessionManagementEnabled = false;

	private boolean frontChannelLogoutEnabled = false;

	public String getIssuer() {
		return this.issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public Duration getAccessTokenValidityDuration() {
		return this.accessTokenValidityDuration;
	}

	public void setAccessTokenValidityDuration(Duration accessTokenValidityDuration) {
		this.accessTokenValidityDuration = accessTokenValidityDuration;
	}

	public Duration getRefreshTokenValidityDuration() {
		return this.refreshTokenValidityDuration;
	}

	public void setRefreshTokenValidityDuration(Duration refreshTokenValidityDuration) {
		this.refreshTokenValidityDuration = refreshTokenValidityDuration;
	}

	public Duration getIdTokenValidityDuration() {
		return this.idTokenValidityDuration;
	}

	public void setIdTokenValidityDuration(Duration idTokenValidityDuration) {
		this.idTokenValidityDuration = idTokenValidityDuration;
	}

	public Period getJwkRetentionPeriod() {
		return this.jwkRetentionPeriod;
	}

	public void setJwkRetentionPeriod(Period jwkRetentionPeriod) {
		this.jwkRetentionPeriod = jwkRetentionPeriod;
	}

	public boolean isSessionManagementEnabled() {
		return this.sessionManagementEnabled;
	}

	public void setSessionManagementEnabled(boolean sessionManagementEnabled) {
		this.sessionManagementEnabled = sessionManagementEnabled;
	}

	public boolean isFrontChannelLogoutEnabled() {
		return this.frontChannelLogoutEnabled;
	}

	public void setFrontChannelLogoutEnabled(boolean frontChannelLogoutEnabled) {
		this.frontChannelLogoutEnabled = frontChannelLogoutEnabled;
	}

	public boolean isSessionManagementOrFrontChannelLogoutEnabled() {
		return isSessionManagementEnabled() || isFrontChannelLogoutEnabled();
	}

}
