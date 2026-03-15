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

package org.axonframework.samples.bank.ai.fraud;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.Iterator;

/**
 * Thread-safe, time-windowed velocity tracker for transaction counts per account.
 * Tracks how many transactions an account has made within a sliding time window.
 * Uses lock-free concurrent data structures for high performance under contention.
 */
public class TransactionVelocityTracker {

    private final long windowMillis;
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>> accountTimestamps =
        new ConcurrentHashMap<>();

    /**
     * @param windowMillis sliding window size in milliseconds (e.g. 60_000 for 1 minute)
     */
    public TransactionVelocityTracker(long windowMillis) {
        this.windowMillis = windowMillis;
    }

    /**
     * Record a transaction for the given account and return the count within the window.
     *
     * @param accountId the account identifier
     * @return the number of transactions in the current sliding window (including this one)
     */
    public int recordAndCount(String accountId) {
        long now = System.currentTimeMillis();
        ConcurrentLinkedDeque<Long> timestamps = accountTimestamps.computeIfAbsent(
            accountId, k -> new ConcurrentLinkedDeque<>());

        timestamps.addLast(now);
        evictExpired(timestamps, now);

        return timestamps.size();
    }

    /**
     * Get the current transaction count for an account without recording a new one.
     */
    public int getCount(String accountId) {
        ConcurrentLinkedDeque<Long> timestamps = accountTimestamps.get(accountId);
        if (timestamps == null) {
            return 0;
        }
        evictExpired(timestamps, System.currentTimeMillis());
        return timestamps.size();
    }

    private void evictExpired(ConcurrentLinkedDeque<Long> timestamps, long now) {
        long cutoff = now - windowMillis;
        Iterator<Long> it = timestamps.iterator();
        while (it.hasNext()) {
            if (it.next() < cutoff) {
                it.remove();
            } else {
                break; // timestamps are ordered; once we hit a valid one, all subsequent are valid
            }
        }
    }
}
