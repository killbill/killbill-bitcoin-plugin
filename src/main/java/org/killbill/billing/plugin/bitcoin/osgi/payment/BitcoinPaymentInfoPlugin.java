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

package org.killbill.billing.plugin.bitcoin.osgi.payment;

import java.math.BigDecimal;
import java.util.UUID;

import org.joda.time.DateTime;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.payment.plugin.api.PaymentInfoPlugin;
import org.killbill.billing.payment.plugin.api.PaymentPluginStatus;

public class BitcoinPaymentInfoPlugin implements PaymentInfoPlugin {

    private final UUID kbPaymentId;
    private final BigDecimal amount;
    private final Currency currency;
    private final DateTime now;
    private final String contractId;

    public BitcoinPaymentInfoPlugin(UUID kbPaymentId, BigDecimal amount, Currency currency, DateTime now, String contractId) {
        this.kbPaymentId = kbPaymentId;
        this.amount = amount;
        this.currency = currency;
        this.now = now;
        this.contractId = contractId;
    }

    @Override
    public UUID getKbPaymentId() {
        return kbPaymentId;
    }

    @Override
    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public Currency getCurrency() {
        return currency;
    }

    @Override
    public DateTime getCreatedDate() {
        return now;
    }

    @Override
    public DateTime getEffectiveDate() {
        return now;
    }

    @Override
    public PaymentPluginStatus getStatus() {
        return PaymentPluginStatus.PENDING;
    }

    @Override
    public String getGatewayError() {
        return null;
    }

    @Override
    public String getGatewayErrorCode() {
        return null;
    }

    @Override
    public String getFirstPaymentReferenceId() {
        return null;
    }

    @Override
    public String getSecondPaymentReferenceId() {
        return contractId;
    }
}
