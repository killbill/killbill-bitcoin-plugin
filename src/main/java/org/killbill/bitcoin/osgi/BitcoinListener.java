/*
 * Copyright 2010-2013 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.bitcoin.osgi;

import java.util.UUID;

import org.osgi.service.log.LogService;

import com.ning.billing.account.api.Account;
import com.ning.billing.account.api.AccountApiException;
import com.ning.billing.notification.plugin.api.ExtBusEvent;
import com.ning.billing.util.callcontext.TenantContext;
import com.ning.killbill.osgi.libs.killbill.OSGIKillbillAPI;
import com.ning.killbill.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIKillbillEventHandler;
import com.ning.killbill.osgi.libs.killbill.OSGIKillbillLogService;

public class BitcoinListener implements OSGIKillbillEventHandler {

    private final LogService logService;
    private final OSGIKillbillAPI osgiKillbillAPI;

    public BitcoinListener(final OSGIKillbillLogService logService, final OSGIKillbillAPI killbillAPI) {
        this.logService = logService;
        this.osgiKillbillAPI = killbillAPI;
    }

    @Override
    public void handleKillbillEvent(final ExtBusEvent killbillEvent) {
        logService.log(LogService.LOG_INFO, "Received event " + killbillEvent.getEventType() +
                                            " for object id " + killbillEvent.getObjectId() +
                                            " of type " + killbillEvent.getObjectType());
        try {
            final Account account = osgiKillbillAPI.getAccountUserApi().getAccountById(killbillEvent.getAccountId(), new BitcoinContext(killbillEvent.getTenantId()));
            logService.log(LogService.LOG_INFO, "Account information: " + account);
        } catch (AccountApiException e) {
            logService.log(LogService.LOG_WARNING, "Unable to find account", e);
        }
    }

    private static final class BitcoinContext implements TenantContext {

        private final UUID tenantId;

        private BitcoinContext(final UUID tenantId) {
            this.tenantId = tenantId;
        }

        @Override
        public UUID getTenantId() {
            return tenantId;
        }
    }
}
