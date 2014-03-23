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

import org.killbill.billing.notification.plugin.api.ExtBusEvent;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentApiException;
import org.killbill.billing.payment.api.PaymentMethod;
import org.killbill.billing.payment.api.PaymentStatus;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIKillbillEventHandler;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillLogService;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class KillbillListener implements OSGIKillbillEventHandler {

    private static final Logger log = LoggerFactory.getLogger(KillbillListener.class);

    private final ImmutableList<String> BITCOIN_PLUGIN_NAMES;

    private final LogService logService;
    private final OSGIKillbillAPI osgiKillbillAPI;
    private final TransactionManager transactionManager;

    public KillbillListener(final OSGIKillbillLogService logService, final OSGIKillbillAPI killbillAPI, final TransactionManager transactionManager,
                            final BitcoinConfig config) {
        this.logService = logService;
        this.osgiKillbillAPI = killbillAPI;
        this.transactionManager = transactionManager;
        final ImmutableList.Builder tmp = ImmutableList.<String>builder();
        for (final String plugin : config.getKillbillBitcoinPlugins()) {
            tmp.add(plugin);
        }
        this.BITCOIN_PLUGIN_NAMES = tmp.build();

        Joiner join = Joiner.on(",");
        log.info("KillbillListener listening :" + join.join(BITCOIN_PLUGIN_NAMES));
    }


    @Override
    public void handleKillbillEvent(final ExtBusEvent killbillEvent) {

        switch (killbillEvent.getEventType()) {
            case PAYMENT_SUCCESS:

                logService.log(LogService.LOG_DEBUG, "Received event " + killbillEvent.getEventType() +
                                                     " for object id " + killbillEvent.getObjectId() +
                                                     " of type " + killbillEvent.getObjectType());

                handlePaymentNotification(killbillEvent);
                break;
            default:
                // ignore
                break;
        }
    }

    private void handlePaymentNotification(final ExtBusEvent paymentEvent) {
        try {

            final BitcoinTenantContext context = new BitcoinTenantContext(paymentEvent.getTenantId());
            final Payment payment = osgiKillbillAPI.getPaymentApi().getPayment(paymentEvent.getObjectId(), true, context);
            final PaymentMethod paymentMethod = osgiKillbillAPI.getPaymentApi().getPaymentMethodById(payment.getPaymentMethodId(), false, false, context);

            // Only care about registered bitcoin plugins
            if (!BITCOIN_PLUGIN_NAMES.contains(paymentMethod.getPluginName())) {
                log.info("KillbillListener filtering out (not a bitoin paymentMethod) payment " + paymentEvent.getObjectId());
                return;
            }

            if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
                log.info("KillbillListener filtering out (not in PENDING state) payment " + paymentEvent.getObjectId());
                return;
            }

            final String bitcoinTransactionId = payment.getPaymentInfoPlugin().getFirstPaymentReferenceId();
            final String bitcoinContractId = payment.getPaymentInfoPlugin().getSecondPaymentReferenceId();

            log.info("KillbillListener registering payment " + paymentEvent.getObjectId() + ", txHash = " + bitcoinTransactionId + ", contractId = " + bitcoinContractId);
            transactionManager.registerPendingPayment(new PendingPayment(paymentEvent.getObjectId(), paymentEvent.getAccountId(), paymentEvent.getTenantId(), bitcoinTransactionId, bitcoinContractId));
        } catch (PaymentApiException e) {
            logService.log(LogService.LOG_WARNING, "Unable to retrieve payment " + paymentEvent.getObjectId(), e);
        }
    }
}
