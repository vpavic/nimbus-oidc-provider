package io.github.vpavic.op.key;

import java.util.Objects;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class KeyInitializer implements CommandLineRunner {

	private final KeyService keyService;

	public KeyInitializer(KeyService keyService) {
		Objects.requireNonNull(keyService, "keyService must not be null");

		this.keyService = keyService;
	}

	@Override
	@Transactional
	public void run(String... args) throws Exception {
		if (this.keyService.findAll().isEmpty()) {
			this.keyService.rotate();
		}
	}

}
