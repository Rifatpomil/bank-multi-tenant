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

package org.axonframework.samples.bank.ai.categorization;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository for querying categorized transactions per tenant and account.
 */
public interface CategorizedTransactionRepository extends JpaRepository<CategorizedTransactionEntry, Long> {

    List<CategorizedTransactionEntry> findByTenantIdAndAccountIdOrderByTimestampMillisDesc(
        String tenantId, String accountId, Pageable pageable);

    List<CategorizedTransactionEntry> findByTenantIdAndAccountIdAndCategoryOrderByTimestampMillisDesc(
        String tenantId, String accountId, TransactionCategory category, Pageable pageable);
}
