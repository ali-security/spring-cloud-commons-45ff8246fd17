/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.context.environment;

import java.util.Collections;

import org.junit.Test;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.springframework.cloud.context.environment.WritableEnvironmentEndpointWebExtension.VALID_KEYS_REGEX_PROPERTY;

public class WritableEnvironmentEndpointWebExtensionTests {

	@Test
	public void writeRejectedWhenValidKeysRegexNotSet() {
		MockEnvironment environment = new MockEnvironment();
		WritableEnvironmentEndpointWebExtension extension = create(environment);

		assertWriteRejected(extension, environment, "spring.cloud.bootstrap.location");
		assertWriteRejected(extension, environment, "logging.config");
		assertWriteRejected(extension, environment, "message");
	}

	@Test
	public void writeRejectedWhenValidKeysRegexIsBlank() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty(VALID_KEYS_REGEX_PROPERTY, " ");
		WritableEnvironmentEndpointWebExtension extension = create(environment);

		assertWriteRejected(extension, environment, "message");
	}

	@Test
	public void writeRejectedWhenKeyDoesNotMatchValidKeysRegex() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty(VALID_KEYS_REGEX_PROPERTY, "mess.ge|delay");
		WritableEnvironmentEndpointWebExtension extension = create(environment);

		assertWriteRejected(extension, environment, "xmessage");
		assertWriteRejected(extension, environment, "message.suffix");
		assertWriteRejected(extension, environment, "delayed");
		assertWriteRejected(extension, environment, "spring.cloud.bootstrap.location");
	}

	@Test
	public void writeAllowedWhenKeyMatchesValidKeysRegex() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty(VALID_KEYS_REGEX_PROPERTY, "mess.ge|delay");
		WritableEnvironmentEndpointWebExtension extension = create(environment);

		Object result = extension.write("message", "Foo");

		then(result).isEqualTo(Collections.singletonMap("message", "Foo"));
		then(environment.getProperty("message")).isEqualTo("Foo");
	}

	@Test
	public void validKeysRegexCannotBeWidenedAtRuntime() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty(VALID_KEYS_REGEX_PROPERTY, "message|management\\..*");
		WritableEnvironmentEndpointWebExtension extension = create(environment);

		extension.write(VALID_KEYS_REGEX_PROPERTY, ".*");

		then(environment.getProperty(VALID_KEYS_REGEX_PROPERTY)).isEqualTo(".*");
		assertWriteRejected(extension, environment, "logging.config");
	}

	@Test
	public void writeRejectedWhenEnvironmentManagerMissing() {
		WritableEnvironmentEndpoint endpoint = new WritableEnvironmentEndpoint(
				new MockEnvironment());
		WritableEnvironmentEndpointWebExtension extension = new WritableEnvironmentEndpointWebExtension(
				endpoint, null);

		try {
			extension.write("message", "Foo");
			fail("expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			then(e.getMessage()).isEqualTo("Invalid key message");
		}
	}

	private void assertWriteRejected(WritableEnvironmentEndpointWebExtension extension,
			MockEnvironment environment, String name) {
		String before = environment.getProperty(name);
		try {
			extension.write(name, "injected");
			fail("expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			then(e.getMessage()).isEqualTo("Invalid key " + name);
		}
		then(environment.getProperty(name)).isEqualTo(before);
	}

	private WritableEnvironmentEndpointWebExtension create(MockEnvironment env) {
		EnvironmentManager manager = new EnvironmentManager(env);
		manager.setApplicationEventPublisher(mock(ApplicationEventPublisher.class));
		WritableEnvironmentEndpoint endpoint = new WritableEnvironmentEndpoint(env);
		return new WritableEnvironmentEndpointWebExtension(endpoint, manager);
	}

}
