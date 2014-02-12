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

package org.killbill.bitcoin.osgi.http;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import com.ning.billing.BillingExceptionBase;
import com.ning.billing.ObjectType;
import com.ning.billing.account.api.Account;
import com.ning.billing.account.api.AccountApiException;
import com.ning.billing.catalog.api.BillingPeriod;
import com.ning.billing.catalog.api.Catalog;
import com.ning.billing.catalog.api.CatalogApiException;
import com.ning.billing.catalog.api.CatalogUserApi;
import com.ning.billing.catalog.api.Currency;
import com.ning.billing.catalog.api.Plan;
import com.ning.billing.catalog.api.PlanPhase;
import com.ning.billing.catalog.api.PlanPhaseSpecifier;
import com.ning.billing.entitlement.api.Entitlement;
import com.ning.billing.entitlement.api.EntitlementApiException;
import com.ning.billing.payment.api.PaymentApiException;
import com.ning.billing.payment.api.PaymentMethod;
import com.ning.billing.tenant.api.Tenant;
import com.ning.billing.util.api.CustomFieldApiException;
import com.ning.billing.util.api.TagApiException;
import com.ning.billing.util.callcontext.CallContext;
import com.ning.billing.util.callcontext.TenantContext;
import com.ning.billing.util.customfield.CustomField;
import com.ning.billing.util.tag.ControlTagType;
import com.ning.killbill.osgi.libs.killbill.OSGIKillbillAPI;
import org.bitcoin.protocols.payments.Protos;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.killbill.bitcoin.osgi.BitcoinActivator;
import org.killbill.bitcoin.osgi.BitcoinCallContext;
import org.killbill.bitcoin.osgi.BitcoinManager;
import org.killbill.bitcoin.osgi.PendingPayment;
import org.killbill.bitcoin.osgi.dao.PendingPaymentDao;
import org.killbill.bitcoin.osgi.payment.BitcoinPaymentPluginApi;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class PaymentRequestServlet extends HttpServlet {


    private final static String BTC_SERVLET_BASE_PATH = "/plugins/" + BitcoinActivator.PLUGIN_NAME;

    private final static String BTC_SUBSCRIPTION_CONTRACT_PATH = BTC_SERVLET_BASE_PATH + "/contract";
    private final static String BTC_SUBSCRIPTION_POLLING_PATH = BTC_SERVLET_BASE_PATH + "/polling";
    private final static String BTC_SUBSCRIPTION_PAYMENT_PATH = BTC_SERVLET_BASE_PATH + "/payment";

    private final static long BTC_TO_SATOSHIS = (1000L * 1000L * 100L);
    private static final String HDR_CREATED_BY = "X-Killbill-CreatedBy";
    private static final String HDR_REASON = "X-Killbill-Reason";
    private static final String HDR_COMMENT = "X-Killbill-Comment";


    private final OSGIKillbillAPI killbillAPI;
    private final PendingPaymentDao paymentDao;
    private final BitcoinManager bitcoinManager;

    public PaymentRequestServlet(OSGIKillbillAPI killbillAPI, PendingPaymentDao paymentDao, BitcoinManager bitcoinManager) {
        this.killbillAPI = killbillAPI;
        this.paymentDao = paymentDao;
        this.bitcoinManager = bitcoinManager;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {

        try {
            final String servletPath = req.getServletPath();
            if (servletPath.equals(BTC_SUBSCRIPTION_PAYMENT_PATH)) {
                createPayment(req, resp);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (BillingExceptionBase e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void createPayment(HttpServletRequest req, HttpServletResponse resp) throws IOException, BillingExceptionBase {
        final Protos.Payment payment = Protos.Payment.parseFrom(req.getInputStream());
        final UUID contractId = UUID.fromString(new String(payment.getMerchantData().toByteArray()));
        Preconditions.checkState(payment.getTransactionsCount() == 1, "Single output transactions for now");

        final org.bitcoinj.wallet.Protos.Transaction transaction = org.bitcoinj.wallet.Protos.Transaction.parseFrom(payment.getTransactionsList().get(0));
        for (org.bitcoinj.wallet.Protos.TransactionOutput output : transaction.getTransactionOutputList()) {
            // PIERRE TODO
            bitcoinManager.getKeys();
            if (true) {
                final List<PendingPayment> pendingPayments = paymentDao.getByBtcContractId(contractId);
                // For now, we take the first one
                final PendingPayment pendingPayment = pendingPayments.get(0);

                // PIERRE TODO
                // broadcast transaction via bitcoinj

                // PIERRE TODO
                paymentDao.update(pendingPayment.getRecordId(), null);

                final Protos.PaymentACK paymentAck = Protos.PaymentACK.newBuilder()
                        .setPayment(payment)
                        .setMemo("Kill Bill payment id " + pendingPayment.getPaymentId())
                        .build();
                paymentAck.writeTo(resp.getOutputStream());
                resp.setContentType("application/bitcoin-paymentack");
                resp.setStatus(HttpServletResponse.SC_OK);

                break;
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            final String servletPath = req.getServletPath();
            if (servletPath.equals(BTC_SUBSCRIPTION_CONTRACT_PATH)) {
                createSubscriptionContract(req, resp);
            } else if (servletPath.equals(BTC_SUBSCRIPTION_POLLING_PATH)) {
                pollForPayment(req, resp);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (BillingExceptionBase e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void createSubscriptionContract(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException, CatalogApiException, AccountApiException, PaymentApiException, EntitlementApiException, CustomFieldApiException, TagApiException {

        final String network = Objects.firstNonNull(req.getParameter("network"), "main");
        final String accountId = req.getParameter("accountId");
        final String product = req.getParameter("product");
        final String billingPeriod = Objects.firstNonNull(req.getParameter("billingPeriod"), "MONTHLY");
        final String priceList = Objects.firstNonNull(req.getParameter("priceList"), "DEFAULT");
        final String externalKey = Objects.firstNonNull(req.getParameter("externalKey"), UUID.randomUUID().toString());

        final DateTime now = new DateTime(DateTimeZone.UTC);
        final CallContext callContext = createCallContext(req, resp);

        final Account account = killbillAPI.getAccountUserApi().getAccountById(UUID.fromString(accountId), callContext);
        final UUID paymentMethodId;
        if (account.getPaymentMethodId() != null) {
            final PaymentMethod defaultPaymentMethod = killbillAPI.getPaymentApi().getPaymentMethodById(account.getPaymentMethodId(), false, false, callContext);
            Preconditions.checkState(defaultPaymentMethod.getPluginName().equals(BitcoinActivator.PLUGIN_NAME));
            paymentMethodId = account.getPaymentMethodId();
        } else {
            paymentMethodId = killbillAPI.getPaymentApi().addPaymentMethod(BitcoinActivator.PLUGIN_NAME, account, true, null, callContext);
        }
        final UUID contractId = UUID.randomUUID();

        final Plan plan = getCatalog(callContext).findPlan(product, BillingPeriod.valueOf(billingPeriod), priceList, now, now);
        createSubscription(account, contractId, plan, externalKey, priceList, now, callContext);

        final long maxPayment = getMaxPaymentAmount(plan);
        Protos.RecurringPaymentDetails recurringPaymentDetails = Protos.RecurringPaymentDetails.newBuilder()
                .setPollingUrl(createURL(req, BTC_SUBSCRIPTION_POLLING_PATH, ImmutableMap.<String, String>of("contractId", contractId.toString(), "network", network)))
                .setPaymentFrequencyType(getPaymentFrequencyType(plan.getBillingPeriod()))
                .setMaxPaymentPerPeriod(maxPayment)
                .setMaxPaymentAmount(maxPayment)
                .build();

        Protos.PaymentDetails details = Protos.PaymentDetails.newBuilder()
                .setNetwork(network)
                .setTime(now.getMillis())
                .setExpires(now.plusDays(1).getMillis())
                .setMemo("Kill Bill subscription " + plan.getName())
                .setPaymentUrl(createURL(req, BTC_SUBSCRIPTION_PAYMENT_PATH))
                .setMerchantData(ByteString.copyFrom(contractId.toString().getBytes()))
                .setSerializedRecurringPaymentDetails(recurringPaymentDetails.toByteString())
                .build();

        Protos.PaymentRequest result = Protos.PaymentRequest.newBuilder()
                .setPaymentDetailsVersion(1)
                .setPkiType("none")
                .setPkiData(null)
                .setSerializedPaymentDetails(details.toByteString())
                .setSignature(null)
                .build();

        result.writeTo(resp.getOutputStream());
        resp.setContentType("application/bitcoin-paymentrequest");
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private void pollForPayment(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException, CatalogApiException {
        final String contractIdString = req.getParameter("contractId");
        Preconditions.checkNotNull(contractIdString);
        final UUID contractId = UUID.fromString(contractIdString);
        final String network = Objects.firstNonNull(req.getParameter("network"), "main");

        final List<PendingPayment> pendingPayments = paymentDao.getByBtcContractId(contractId);
        // TODO PIERRE
        final PendingPayment pendingPayment = pendingPayments.get(0);

        final DateTime now = new DateTime(DateTimeZone.UTC);

        Protos.PaymentDetails details = Protos.PaymentDetails.newBuilder()
                .setNetwork(network)
                .setTime(now.getMillis())
                .setExpires(now.plusDays(1).getMillis())
                .setMemo("Kill Bill payment " + pendingPayment.getPaymentId())
                .setPaymentUrl(createURL(req, BTC_SUBSCRIPTION_PAYMENT_PATH))
                .setMerchantData(ByteString.copyFrom(contractId.toString().getBytes()))
                .build();

        Protos.PaymentRequest result = Protos.PaymentRequest.newBuilder()
                .setPaymentDetailsVersion(1)
                .setPkiType("none")
                .setPkiData(null)
                .setSerializedPaymentDetails(details.toByteString())
                .setSignature(null)
                .build();

        result.writeTo(resp.getOutputStream());
        resp.setContentType("application/bitcoin-paymentrequest");
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private String createURL(final HttpServletRequest req, final String path) {
        return createURL(req, path, ImmutableMap.<String, String>of());
    }

    private String createURL(HttpServletRequest req, String path, ImmutableMap<String, String> params) {
        final StringBuilder queryParamsBuilder = new StringBuilder("?");
        for (final String key : params.keySet()) {
            queryParamsBuilder.append(key)
                    .append("=")
                    .append(params.get(key))
                    .append("&");
        }

        return req.getScheme() + "://" + req.getRemoteHost() + ":" + req.getRemotePort() + path + queryParamsBuilder.toString();
    }

    private Protos.PaymentFrequencyType getPaymentFrequencyType(BillingPeriod term) {
        switch (term) {
            case MONTHLY:
                return Protos.PaymentFrequencyType.MONTHLY;
            case ANNUAL:
                return Protos.PaymentFrequencyType.ANNUAL;
            case QUARTERLY:
                return Protos.PaymentFrequencyType.QUARTERLY;
            default:
                throw new RuntimeException("Unsupported billing period " + term);
        }
    }

    private long getMaxPaymentAmount(final Plan plan) throws CatalogApiException {

        BigDecimal maxPaymentAmount = BigDecimal.ZERO;
        for (PlanPhase ph : plan.getAllPhases()) {
            BigDecimal phaseAmount = ph.getRecurringPrice().getPrice(Currency.BTC);
            if (maxPaymentAmount.compareTo(phaseAmount) < 0) {
                maxPaymentAmount = phaseAmount;
            }
        }
        return maxPaymentAmount.longValue() * BTC_TO_SATOSHIS;
    }

    private CallContext createCallContext(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        final String createdBy = Objects.firstNonNull(req.getHeader(HDR_CREATED_BY), req.getRemoteAddr());
        final String reason = req.getHeader(HDR_REASON);
        final String comment = Objects.firstNonNull(req.getHeader(HDR_COMMENT), req.getRequestURI());

        // Set by the TenantFilter
        final Tenant tenant = (Tenant) req.getAttribute("killbill_tenant");
        UUID tenantId = null;
        if (tenant != null) {
            tenantId = tenant.getId();
        }
        return new BitcoinCallContext(tenantId, reason, comment);
    }

    private Catalog getCatalog(final TenantContext context) {
        final CatalogUserApi catalogUserApi = killbillAPI.getCatalogUserApi();
        Preconditions.checkNotNull(catalogUserApi);
        return catalogUserApi.getCatalog(null, context);
    }

    private void createSubscription(final Account account, final UUID contractId, final Plan plan, final String externalKey, final String priceList, final DateTime now, final CallContext callContext) throws EntitlementApiException, TagApiException, CustomFieldApiException {

        killbillAPI.getTagUserApi().addTag(account.getId(), ObjectType.ACCOUNT, ControlTagType.AUTO_PAY_OFF.getId(), callContext);

        final PlanPhaseSpecifier spec = new PlanPhaseSpecifier(plan.getProduct().getName(), plan.getProduct().getCategory(), plan.getBillingPeriod(), priceList, null);
        final Entitlement entitlement = killbillAPI.getEntitlementApi().createBaseEntitlement(account.getId(), spec, externalKey, now.toLocalDate(), callContext);

        killbillAPI.getCustomFieldUserApi().addCustomFields(ImmutableList.<CustomField>of(new CustomField() {
            @Override
            public UUID getObjectId() {
                return entitlement.getId();
            }

            @Override
            public ObjectType getObjectType() {
                return ObjectType.SUBSCRIPTION;
            }

            @Override
            public String getFieldName() {
                return BitcoinPaymentPluginApi.CONTRACT_FIELD_NAME;
            }

            @Override
            public String getFieldValue() {
                return contractId.toString();
            }

            @Override
            public UUID getId() {
                return null;
            }

            @Override
            public DateTime getCreatedDate() {
                return now;
            }

            @Override
            public DateTime getUpdatedDate() {
                return now;
            }
        }), callContext);

        killbillAPI.getTagUserApi().removeTag(account.getId(), ObjectType.ACCOUNT, ControlTagType.AUTO_PAY_OFF.getId(), callContext);

    }
}
