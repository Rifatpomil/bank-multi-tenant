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

import org.axonframework.samples.bank.ai.TransactionContext;

/**
 * Strategy interface for transaction categorization.
 * Implementations classify transactions into categories based on patterns.
 */
public interface CategorizationService {

    /**
     * Categorize a transaction based on its context.
     *
     * @param context the transaction details
     * @return the categorization result with confidence
     */
    CategoryResult categorize(TransactionContext context);
}
