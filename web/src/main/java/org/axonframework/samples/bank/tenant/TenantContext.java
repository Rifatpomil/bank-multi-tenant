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

package org.axonframework.samples.bank.tenant;

/**
 * Thread-local holder for the current tenant ID.
 * Used for multi-tenant isolation: commands and queries are scoped by tenant.
 * Set from request headers (X-Tenant-ID) or STOMP message headers.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = ThreadLocal.withInitial(
        () -> org.axonframework.samples.bank.tenant.TenantConstants.DEFAULT_TENANT);

    private TenantContext() {
    }

    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId != null && !tenantId.isEmpty() ? tenantId
            : org.axonframework.samples.bank.tenant.TenantConstants.DEFAULT_TENANT);
    }

    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
