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

import com.google.bitcoin.core.Sha256Hash;
import com.ning.billing.account.api.Account;
import com.ning.billing.account.api.AccountApiException;
import com.ning.billing.payment.api.PaymentApiException;
import com.ning.billing.util.callcontext.CallContext;
import com.ning.killbill.osgi.libs.killbill.OSGIKillbillAPI;
import org.osgi.service.log.LogService;

import java.util.concurrent.ConcurrentHashMap;

public class TransactionManager {

    private final LogService logService;
    private final OSGIKillbillAPI osgiKillbillAPI;
    private final BitcoinConfig config;

    /* Map between pending bitcoin transactions and killbill pending payments */
    private final ConcurrentHashMap<Sha256Hash, PendingPayment> pendingTransactions;


    public TransactionManager(final LogService logService, final OSGIKillbillAPI osgiKillbillAPI, final BitcoinConfig config) {
        this.logService = logService;
        this.osgiKillbillAPI = osgiKillbillAPI;
        this.config = config;
        this.pendingTransactions = new ConcurrentHashMap<Sha256Hash, PendingPayment>();
    }

    public void registerPendingPayment(final PendingPayment pendingPayment) {
        pendingTransactions.putIfAbsent(pendingPayment.getBtcTxHash(), pendingPayment);
    }

    public void notifyPaymentSystem(final Sha256Hash hash) {

        final PendingPayment pendingPayment = pendingTransactions.get(hash);
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
            pendingTransactions.remove(hash);
        }
    }

    public boolean isPendingTransaction(final Sha256Hash hash) {
        return pendingTransactions.contains(hash);
    }
}
