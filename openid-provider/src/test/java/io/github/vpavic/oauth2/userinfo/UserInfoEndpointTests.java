package io.github.vpavic.oauth2.userinfo;

import com.nimbusds.oauth2.sdk.id.Issuer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import io.github.vpavic.oauth2.OpenIdProviderWebMvcConfiguration;
import io.github.vpavic.oauth2.UserInfoSecurityConfiguration;
import io.github.vpavic.oauth2.client.ClientRepository;
import io.github.vpavic.oauth2.jwk.JwkSetLoader;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Tests for {@link UserInfoEndpoint}.
 *
 * @author Vedran Pavic
 */
@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration
public class UserInfoEndpointTests {

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mvc;

	@Before
	public void setUp() {
		this.mvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
	}

	@Test
	public void test() {
		// TODO
	}

	@Configuration
	@EnableWebMvc
	@EnableWebSecurity
	@Import({ OpenIdProviderWebMvcConfiguration.class, UserInfoSecurityConfiguration.class })
	static class Config {

		@Bean
		public UserDetailsService userDetailsService() {
			return mock(UserDetailsService.class);
		}

		@Bean
		public Issuer issuer() {
			return new Issuer("http://example.com");
		}

		@Bean
		public JwkSetLoader jwkSetLoader() {
			return mock(JwkSetLoader.class);
		}

		@Bean
		public ClientRepository clientRepository() {
			return mock(ClientRepository.class);
		}

		@Bean
		public UserInfoEndpoint userInfoEndpoint() {
			return new UserInfoEndpoint(mock(UserInfoMapper.class));
		}

	}

}
