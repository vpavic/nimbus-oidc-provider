package io.github.vpavic.op.oauth2.token;

import java.util.Collections;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class NullAccessTokenClaimsMapper implements AccessTokenClaimsMapper {

	@Override
	public Map<String, Object> map(String principal) {
		return Collections.emptyMap();
	}

}