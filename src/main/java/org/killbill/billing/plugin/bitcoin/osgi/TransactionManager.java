/*
 * Copyright 2010-2014 Ning, Inc.
 * Copyright 2014 The Billing Project, LLC
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

package org.killbill.billing.plugin.bitcoin.osgi;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.payment.api.PaymentApiException;
import org.killbill.billing.plugin.bitcoin.osgi.dao.PendingPaymentDao;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillAPI;
import org.osgi.service.log.LogService;

public class TransactionManager {

    private final LogService logService;
    private final OSGIKillbillAPI osgiKillbillAPI;
    private final BitcoinConfig config;
    private final PendingPaymentDao dao;

    public TransactionManager(final LogService logService, final OSGIKillbillAPI osgiKillbillAPI, final PendingPaymentDao dao, final BitcoinConfig config) {
        this.logService = logService;
        this.osgiKillbillAPI = osgiKillbillAPI;
        this.config = config;
        this.dao = dao;
    }

    public void registerPendingPayment(final PendingPayment pendingPayment) {
        dao.insertPendingPayment(pendingPayment);
    }

    public boolean notifyPaymentSystemIfExists(final String hash) {

        final PendingPayment pendingPayment = dao.getByBtcTransactionId(hash);
        if (pendingPayment == null) {
            return false;
        }

        final CallContext context = new BitcoinCallContext(pendingPayment.getTenantId(), config.getConfidenceBlockDepth());
        try {
            final Account account = osgiKillbillAPI.getAccountUserApi().getAccountById(pendingPayment.getAccountId(), context);
            osgiKillbillAPI.getPaymentApi().notifyPendingPaymentOfStateChanged(account, pendingPayment.getPaymentId(), true, context);
        } catch (PaymentApiException e) {
            logService.log(LogService.LOG_WARNING, "Failed to notify payment service for bitcoin completion, payment =  " + pendingPayment.getPaymentId());
        } catch (AccountApiException e) {
            logService.log(LogService.LOG_WARNING, "Failed to notify payment service for bitcoin completion, account =  " + pendingPayment.getAccountId());
        } finally {
            // If we fail we still remove it as retrying would probably end up in the same result.
            dao.removePendingPayment(pendingPayment.getPaymentId());
        }
        return true;
    }
}
