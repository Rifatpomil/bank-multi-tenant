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

import org.axonframework.commandhandling.model.Repository;
import org.axonframework.eventsourcing.EventCountSnapshotTriggerDefinition;
import org.axonframework.eventsourcing.EventSourcingRepository;
import org.axonframework.eventsourcing.GenericAggregateFactory;
import org.axonframework.eventsourcing.Snapshotter;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.samples.bank.command.BankAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Snapshotting configuration for aggregates.
 * When enabled, creates an EventSourcingRepository with a snapshot trigger
 * that takes a snapshot every N events (configurable via properties).
 */
@Configuration
@ConditionalOnProperty(name = "axon.snapshotting.enabled", havingValue = "true")
public class SnapshotConfig {

    @Bean
    @Primary
    public Repository<BankAccount> bankAccountRepository(EventStore eventStore,
            Snapshotter snapshotter,
            @Value("${axon.snapshotting.bank-account.threshold:50}") int threshold) {
        EventCountSnapshotTriggerDefinition trigger = new EventCountSnapshotTriggerDefinition(snapshotter, threshold);
        return new EventSourcingRepository<>(new GenericAggregateFactory<>(BankAccount.class),
                eventStore, trigger);
    }
}
