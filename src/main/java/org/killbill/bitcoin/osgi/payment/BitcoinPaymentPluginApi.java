/*
 * Copyright 2010-2014 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.bitcoin.osgi.payment;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.ning.billing.BillingExceptionBase;
import com.ning.billing.ObjectType;
import com.ning.billing.catalog.api.Currency;
import com.ning.billing.invoice.api.Invoice;
import com.ning.billing.payment.api.Payment;
import com.ning.billing.payment.api.PaymentMethodPlugin;
import com.ning.billing.payment.plugin.api.PaymentInfoPlugin;
import com.ning.billing.payment.plugin.api.PaymentMethodInfoPlugin;
import com.ning.billing.payment.plugin.api.PaymentPluginApi;
import com.ning.billing.payment.plugin.api.PaymentPluginApiException;
import com.ning.billing.payment.plugin.api.RefundInfoPlugin;
import com.ning.billing.util.callcontext.CallContext;
import com.ning.billing.util.callcontext.TenantContext;
import com.ning.billing.util.customfield.CustomField;
import com.ning.billing.util.entity.Pagination;
import com.ning.killbill.osgi.libs.killbill.OSGIKillbillAPI;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.killbill.bitcoin.osgi.BitcoinActivator;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class BitcoinPaymentPluginApi implements PaymentPluginApi {

    public static final String CONTRACT_FIELD_NAME = BitcoinActivator.PLUGIN_NAME + "-contract";

    private final OSGIKillbillAPI killbillAPI;

    public BitcoinPaymentPluginApi(final OSGIKillbillAPI killbillAPI) {
        this.killbillAPI = killbillAPI;
    }

    @Override
    public PaymentInfoPlugin processPayment(UUID kbAccountId, UUID kbPaymentId, UUID kbPaymentMethodId, BigDecimal amount, Currency currency, CallContext context) throws PaymentPluginApiException {
        return getPaymentInfo(kbAccountId, kbPaymentId, context);
    }

    @Override
    public PaymentInfoPlugin getPaymentInfo(UUID kbAccountId, UUID kbPaymentId, TenantContext context) throws PaymentPluginApiException {
        try {
            final Payment payment = killbillAPI.getPaymentApi().getPayment(kbPaymentId, false, context);
            final Invoice invoice = killbillAPI.getInvoiceUserApi().getInvoice(payment.getInvoiceId(), context);
            Preconditions.checkState(invoice.getInvoiceItems().size() == 1, "Are you subscription aligned?");

            // TODO broken
            return new BitcoinPaymentInfoPlugin(kbPaymentId, payment.getAmount(), payment.getCurrency(), new DateTime(DateTimeZone.UTC), invoice.getInvoiceItems().get(0).getSubscriptionId().toString());
        } catch (BillingExceptionBase e) {
            throw new PaymentPluginApiException("Error getting the custom field", e);
        }
    }

    @Override
    public Pagination<PaymentInfoPlugin> searchPayments(String searchKey, Long offset, Long limit, TenantContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public RefundInfoPlugin processRefund(UUID kbAccountId, UUID kbPaymentId, BigDecimal refundAmount, Currency currency, CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public List<RefundInfoPlugin> getRefundInfo(UUID kbAccountId, UUID kbPaymentId, TenantContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public Pagination<RefundInfoPlugin> searchRefunds(String searchKey, Long offset, Long limit, TenantContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public void addPaymentMethod(UUID kbAccountId, UUID kbPaymentMethodId, PaymentMethodPlugin paymentMethodProps, boolean setDefault, CallContext context) throws PaymentPluginApiException {

    }

    @Override
    public void deletePaymentMethod(UUID kbAccountId, UUID kbPaymentMethodId, CallContext context) throws PaymentPluginApiException {

    }

    @Override
    public PaymentMethodPlugin getPaymentMethodDetail(UUID kbAccountId, UUID kbPaymentMethodId, TenantContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public void setDefaultPaymentMethod(UUID kbAccountId, UUID kbPaymentMethodId, CallContext context) throws PaymentPluginApiException {

    }

    @Override
    public List<PaymentMethodInfoPlugin> getPaymentMethods(UUID kbAccountId, boolean refreshFromGateway, CallContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public Pagination<PaymentMethodPlugin> searchPaymentMethods(String searchKey, Long offset, Long limit, TenantContext context) throws PaymentPluginApiException {
        return null;
    }

    @Override
    public void resetPaymentMethods(UUID kbAccountId, List<PaymentMethodInfoPlugin> paymentMethods) throws PaymentPluginApiException {

    }
}
