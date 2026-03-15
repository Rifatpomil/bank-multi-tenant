/*
 * Copyright (c) 2016. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.samples.bank.config;

import org.axonframework.commandhandling.SimpleCommandBus;
import org.axonframework.commandhandling.model.Repository;
import org.axonframework.config.SagaConfiguration;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.messaging.interceptors.BeanValidationInterceptor;
import org.axonframework.samples.bank.command.BankAccount;
import org.axonframework.samples.bank.command.BankAccountCommandHandler;
import org.axonframework.samples.bank.command.BankTransferManagementSaga;
import org.axonframework.messaging.correlation.SimpleCorrelationDataProvider;
import org.axonframework.samples.bank.tenant.TenantCommandDispatchInterceptor;
import org.axonframework.samples.bank.tenant.TenantConstants;
import org.axonframework.spring.config.AxonConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class AxonConfig {

    @Autowired
    private AxonConfiguration axonConfiguration;
    @Autowired
    private EventBus eventBus;

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(name = "bankAccountRepository")
    public Repository<BankAccount> defaultBankAccountRepository() {
        return axonConfiguration.repository(BankAccount.class);
    }

    @Bean
    public BankAccountCommandHandler bankAccountCommandHandler(Repository<BankAccount> bankAccountRepository) {
        return new BankAccountCommandHandler(bankAccountRepository, eventBus);
    }

    @Bean
    public SagaConfiguration<BankTransferManagementSaga> bankTransferManagementSagaConfiguration() {
        return SagaConfiguration.trackingSagaManager(BankTransferManagementSaga.class);
    }

    @PostConstruct
    public void registerTenantCorrelation() {
        try {
            axonConfiguration.correlationDataProviders()
                    .add(new SimpleCorrelationDataProvider(TenantConstants.TENANT_ID_KEY));
        } catch (UnsupportedOperationException e) {
            org.slf4j.LoggerFactory.getLogger(AxonConfig.class).warn(
                    "Could not add tenant_id to correlation providers (list is immutable). " +
                            "Events may not carry tenant_id metadata. Consider providing a Configurer bean that calls "
                            +
                            "configureCorrelationDataProviders() with MessageOriginProvider and SimpleCorrelationDataProvider(\"tenant_id\").");
        }
    }

    @Autowired
    public void configure(@Qualifier("localSegment") SimpleCommandBus simpleCommandBus) {
        simpleCommandBus.registerDispatchInterceptor(new TenantCommandDispatchInterceptor());
        simpleCommandBus.registerDispatchInterceptor(new BeanValidationInterceptor<>());
    }
}
