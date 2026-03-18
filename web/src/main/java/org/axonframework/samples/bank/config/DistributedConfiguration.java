package org.axonframework.samples.bank.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Distributed deployment configuration.
 * Placeholder for distributed command bus setup when using a service registry.
 */
@Profile("distributed")
@Configuration
public class DistributedConfiguration {

}
