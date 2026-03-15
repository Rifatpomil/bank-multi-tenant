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

package org.axonframework.samples.bank.web;

import org.axonframework.samples.bank.query.accountdaily.AccountDailySummaryEventListener;
import org.axonframework.samples.bank.query.accountdaily.AccountDailySummaryRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin endpoint to rebuild the AccountDailySummary projection by replaying
 * events.
 * See README for usage. Protect this endpoint in production.
 *
 * Note: In Axon 3.0.x, full tracking processor token reset is not available
 * via public API. This endpoint clears the projection table. For a full
 * replay from the event store, manually delete the processor's tracking
 * token entry and restart the application.
 */
@RestController
@RequestMapping("/admin/projections")
@ConditionalOnProperty(name = "axon.admin.rebuild-enabled", havingValue = "true")
public class ProjectionRebuildController {

    private final AccountDailySummaryRepository accountDailySummaryRepository;

    public ProjectionRebuildController(AccountDailySummaryRepository accountDailySummaryRepository) {
        this.accountDailySummaryRepository = accountDailySummaryRepository;
    }

    /**
     * Rebuild AccountDailySummary: clears the projection table.
     * For a full replay, also delete the tracking token for the processing group
     * from the token_entry table and restart the application.
     */
    @PostMapping("/account-daily-summary/rebuild")
    public ResponseEntity<String> rebuildAccountDailySummary() {
        accountDailySummaryRepository.deleteAll();

        return ResponseEntity.ok("AccountDailySummary table cleared. "
                + "For a full replay, delete the tracking token for processing group '"
                + AccountDailySummaryEventListener.PROCESSING_GROUP
                + "' from the token_entry table and restart the application.");
    }
}
