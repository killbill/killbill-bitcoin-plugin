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

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
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
import com.ning.billing.catalog.api.PhaseType;
import com.ning.billing.catalog.api.Plan;
import com.ning.billing.catalog.api.PlanPhase;
import com.ning.billing.catalog.api.PlanPhaseSpecifier;
import com.ning.billing.entitlement.api.Entitlement;
import com.ning.billing.entitlement.api.EntitlementApiException;
import com.ning.billing.entitlement.api.Subscription;
import com.ning.billing.entitlement.api.SubscriptionApiException;
import com.ning.billing.entitlement.api.SubscriptionBundle;
import com.ning.billing.entitlement.api.SubscriptionEvent;
import com.ning.billing.entitlement.api.SubscriptionEventType;
import com.ning.billing.payment.api.Payment;
import com.ning.billing.payment.api.PaymentApiException;
import com.ning.billing.tenant.api.Tenant;
import com.ning.billing.util.api.CustomFieldApiException;
import com.ning.billing.util.api.TagApiException;
import com.ning.billing.util.callcontext.CallContext;
import com.ning.billing.util.callcontext.TenantContext;
import com.ning.killbill.osgi.libs.killbill.OSGIKillbillAPI;
import org.bitcoin.protocols.payments.Protos;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.killbill.bitcoin.osgi.BitcoinActivator;
import org.killbill.bitcoin.osgi.BitcoinCallContext;
import org.killbill.bitcoin.osgi.BitcoinManager;
import org.killbill.bitcoin.osgi.BitcoinSubscriptionId;
import org.killbill.bitcoin.osgi.Contract;
import org.killbill.bitcoin.osgi.PendingPayment;
import org.killbill.bitcoin.osgi.TransactionLog;
import org.killbill.bitcoin.osgi.dao.ContractDao;
import org.killbill.bitcoin.osgi.dao.PendingPaymentDao;
import org.killbill.bitcoin.osgi.dao.TransactionLogDao;

import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class PaymentRequestServlet extends HttpServlet {

    // TODO configurable (multi-tenant?)
    public static final String DEFAULT_MERCHANT_ID = "org.killbill";

    private final static String BTC_SERVLET_BASE_PATH = "/plugins/" + BitcoinActivator.PLUGIN_NAME;

    private final static String BTC_SUBSCRIPTION_CONTRACT = "/contract";
    private final static String BTC_SUBSCRIPTION_POLLING = "/polling";
    private final static String BTC_SUBSCRIPTION_PAYMENT = "/payment";
    private final static String BTC_WALLET = "/wallet";

    private final static String BTC_SUBSCRIPTION_CONTRACT_PATH = BTC_SERVLET_BASE_PATH + BTC_SUBSCRIPTION_CONTRACT;
    private final static String BTC_SUBSCRIPTION_POLLING_PATH = BTC_SERVLET_BASE_PATH + BTC_SUBSCRIPTION_POLLING;
    private final static String BTC_SUBSCRIPTION_PAYMENT_PATH = BTC_SERVLET_BASE_PATH + BTC_SUBSCRIPTION_PAYMENT;

    private final static long BTC_TO_SATOSHIS = (1000L * 1000L * 100L);
    private static final String HDR_CREATED_BY = "X-Killbill-CreatedBy";
    private static final String HDR_REASON = "X-Killbill-Reason";
    private static final String HDR_COMMENT = "X-Killbill-Comment";


    private final OSGIKillbillAPI killbillAPI;
    private final ContractDao contractDao;
    private final PendingPaymentDao paymentDao;
    private final BitcoinManager bitcoinManager;
    private final TransactionLogDao transactionLogDao;

    public PaymentRequestServlet(OSGIKillbillAPI killbillAPI, ContractDao contractDao, PendingPaymentDao paymentDao, TransactionLogDao transactionLogDao, BitcoinManager bitcoinManager) {
        this.killbillAPI = killbillAPI;
        this.contractDao = contractDao;
        this.paymentDao = paymentDao;
        this.transactionLogDao = transactionLogDao;
        this.bitcoinManager = bitcoinManager;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {

        try {
            final String pathInfo = req.getPathInfo();
            if (pathInfo.equals(BTC_SUBSCRIPTION_CONTRACT)) {
                createContract(req, resp);
            } else if (pathInfo.equals(BTC_SUBSCRIPTION_POLLING)) {
                pollForPayment(req, resp);
            } else if (pathInfo.equals(BTC_WALLET)) {
                dumpWallet(req, resp);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (BillingExceptionBase e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void dumpWallet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.getOutputStream().write(bitcoinManager.walletAsString().getBytes("UTF-8"));
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            final String pathInfo = req.getPathInfo();
            if (pathInfo.equals(BTC_SUBSCRIPTION_PAYMENT)) {
                createPayment(req, resp);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (BillingExceptionBase e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } catch (InterruptedException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (ExecutionException e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }


    private void createPayment(HttpServletRequest req, HttpServletResponse resp) throws IOException, BillingExceptionBase, ExecutionException, InterruptedException {
        final Protos.Payment payment = Protos.Payment.parseFrom(req.getInputStream());
        final UUID contractId = UUID.fromString(new String(payment.getMerchantData().toByteArray()));
        // TODO for now just accept one transaction per payment
        Preconditions.checkState(payment.getTransactionsCount() == 1, "Single output transactions for now");

        final List<PendingPayment> pendingPayments = paymentDao.getByBtcContractId(contractId);
        // For now, we take the first one
        final PendingPayment pendingPayment = pendingPayments.size() > 0 ? pendingPayments.get(0) : null;
        if (pendingPayment == null) {
            // Nothing to pay
            resp.setStatus(HttpServletResponse.SC_GONE);
            return;
        }

        transactionLogDao.insertTransactionLog(new TransactionLog(new DateTime(DateTimeZone.UTC), "createPayment", pendingPayment.getAccountId(), null, contractId));
        final List<ByteString> transactionList = payment.getTransactionsList();
        //Collection<TransactionOutput> outputs = bitcoinManager.isMine(transactionList.get(0));
        //Preconditions.checkState(outputs.size() == 1, "Expecting one");

        final Transaction broadcastedTransaction = bitcoinManager.broadcastTransaction(transactionList.get(0));

        bitcoinManager.commitTransaction(broadcastedTransaction);

        // PIERRE TODO We cannot solely rely on transactionId because of Malleability issues -- https://en.bitcoin.it/wiki/Transaction_Malleability
        // We should really track transaction with inputs, outputs -- which contain the address
        paymentDao.update(pendingPayment.getRecordId(), broadcastedTransaction.getHash().toString());

        final Protos.PaymentACK paymentAck = Protos.PaymentACK.newBuilder()
                .setPayment(payment)
                .setMemo("Kill Bill payment id " + pendingPayment.getPaymentId())
                .build();
        paymentAck.writeTo(resp.getOutputStream());
        resp.setContentType("application/bitcoin-paymentack");
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private void createContract(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException, CatalogApiException, AccountApiException, PaymentApiException, EntitlementApiException, CustomFieldApiException, TagApiException, SubscriptionApiException {

        final String networkArg = Objects.firstNonNull(req.getParameter("network"), "main");
        final String contractIdArg = req.getParameter("contractId");

        // TODO The request parameter should be a subscriptionId, but the wallet should be given btcSubscriptionId.
        final String bitcoinSubscriptionIdArg = req.getParameter("subscriptionId");

        final DateTime nowDateTime = new DateTime(DateTimeZone.UTC);
        final LocalDate now = new LocalDate(nowDateTime);
        final CallContext callContext = createCallContext(req, resp);

        final BitcoinSubscriptionId bitcoinSubscriptionId = BitcoinSubscriptionId.fromString(bitcoinSubscriptionIdArg);
        // TODO Works only for subscription aligned - exercise for the reader for bundle / account aligned
        // Depending on the alignment, the contracts should contain one contract for each entity that can end up in one invoice.
        // For example, all subscriptions on a given bundle, or all account aligned subscriptions
        Preconditions.checkState(ObjectType.SUBSCRIPTION.equals(bitcoinSubscriptionId.getAlignment()));

        final UUID subscriptionId = bitcoinSubscriptionId.getEntityId();
        final Subscription subscription = killbillAPI.getSubscriptionApi().getSubscriptionForEntitlementId(subscriptionId, callContext);
        final SubscriptionBundle bundle = killbillAPI.getSubscriptionApi().getSubscriptionBundle(subscription.getBundleId(), callContext);

        SubscriptionEvent currentEvent = null;
        for (final SubscriptionEvent subscriptionEvent : Lists.reverse(bundle.getTimeline().getSubscriptionEvents())) {
            if (subscriptionEvent.getEntitlementId().equals(subscription.getId()) &&
                    subscriptionEvent.getEffectiveDate().compareTo(now) <= 0 &&
                    (SubscriptionEventType.START_BILLING.equals(subscriptionEvent.getSubscriptionEventType()) || SubscriptionEventType.CHANGE.equals(subscriptionEvent.getSubscriptionEventType()))) {
                currentEvent = subscriptionEvent;
                break;
            }
        }

        final SubscriptionEvent futureChangeOrCancelEvent = Iterables.<SubscriptionEvent>tryFind(bundle.getTimeline().getSubscriptionEvents(), new Predicate<SubscriptionEvent>() {
            @Override
            public boolean apply(SubscriptionEvent input) {
                return input.getEntitlementId().equals(subscription.getId()) &&
                        (SubscriptionEventType.CHANGE.equals(input.getSubscriptionEventType()) || SubscriptionEventType.STOP_BILLING.equals(input.getSubscriptionEventType())) &&
                        // TODO clock
                        input.getEffectiveDate().compareTo(new LocalDate(new DateTime(DateTimeZone.UTC))) > 0;
            }
        }).orNull();

        final boolean isCancelled = (subscription.getBillingEndDate() != null && subscription.getBillingEndDate().compareTo(new LocalDate(now)) <= 0);
        final long maxPayment = isCancelled ? 0L : getMaxPaymentAmount(currentEvent.getNextPlan());
        final Protos.PaymentFrequencyType frequencyType = isCancelled ? null : getPaymentFrequencyType(subscription.getLastActivePlan().getBillingPeriod());

        final List<Protos.RecurringPaymentContract> contracts = new LinkedList<Protos.RecurringPaymentContract>();

        final UUID contractId = contractIdArg == null ? UUID.randomUUID() : UUID.fromString(contractIdArg);
        Protos.RecurringPaymentContract.Builder currentContractBuilder = Protos.RecurringPaymentContract.newBuilder()
                .setContractId(uuidToByteString(contractId))
                .setStarts(localDateToMillis(currentEvent.getEffectiveDate()))
                .setPollingUrl(createURL(req, BTC_SUBSCRIPTION_POLLING_PATH, ImmutableMap.<String, String>of("merchantId", DEFAULT_MERCHANT_ID, "subscriptionId", bitcoinSubscriptionId.toString(), "contractId", contractId.toString(), "network", networkArg)))
                .setPaymentFrequencyType(frequencyType)
                .setMaxPaymentPerPeriod(maxPayment)
                .setMaxPaymentAmount(maxPayment);

        if (futureChangeOrCancelEvent != null) {
            currentContractBuilder.setEnds(localDateToMillis(futureChangeOrCancelEvent.getEffectiveDate()));

            // TODO check it may exist!
            final UUID nextContractId = UUID.randomUUID();
            transactionLogDao.insertTransactionLog(new TransactionLog(new DateTime(DateTimeZone.UTC), "createContract", subscription.getAccountId(), subscription.getId(), nextContractId));

            final long nextMaxAmount = getMaxPaymentAmount(futureChangeOrCancelEvent.getNextPlan());
            Protos.RecurringPaymentContract.Builder nextContractBuilder = Protos.RecurringPaymentContract.newBuilder()
                    .setContractId(uuidToByteString(nextContractId))
                    .setStarts(localDateToMillis(futureChangeOrCancelEvent.getEffectiveDate()))
                    .setEnds(localDateToMillis(subscription.getBillingEndDate()))
                    .setPollingUrl(createURL(req, BTC_SUBSCRIPTION_POLLING_PATH, ImmutableMap.<String, String>of("merchantId", DEFAULT_MERCHANT_ID, "subscriptionId", bitcoinSubscriptionId.toString(), "contractId", nextContractId.toString(), "network", networkArg)))
                    .setMaxPaymentPerPeriod(nextMaxAmount)
                    .setMaxPaymentAmount(nextMaxAmount);
            if (futureChangeOrCancelEvent.getNextPlan() != null) {
                nextContractBuilder.setPaymentFrequencyType(getPaymentFrequencyType(futureChangeOrCancelEvent.getNextPlan().getBillingPeriod()));
            }

            contracts.add(nextContractBuilder.build());
        }

        contracts.add(currentContractBuilder.build());

        if (contractIdArg == null) {
            contractDao.insertContract(new Contract(new BitcoinSubscriptionId(ObjectType.SUBSCRIPTION, subscriptionId), currentEvent.getEffectiveDate(), futureChangeOrCancelEvent == null ? null : futureChangeOrCancelEvent.getEffectiveDate(), contractId));
            transactionLogDao.insertTransactionLog(new TransactionLog(new DateTime(DateTimeZone.UTC), "createContract", subscription.getAccountId(), subscription.getId(), contractId));
        }

        Protos.RecurringPaymentDetails recurringPaymentDetails = Protos.RecurringPaymentDetails.newBuilder()
                .setMerchantId(DEFAULT_MERCHANT_ID)
                .setSubscriptionId(uuidToByteString(subscription.getId()))
                .addAllContracts(contracts)
                .build();

        Protos.PaymentDetails details = Protos.PaymentDetails.newBuilder()
                .setNetwork(networkArg)
                .setTime(nowDateTime.getMillis())
                .setExpires(nowDateTime.plusDays(1).getMillis())
                .setMemo("Kill Bill subscription " + subscription.getLastActivePlan().getName())
                .setPaymentUrl(createURL(req, BTC_SUBSCRIPTION_PAYMENT_PATH))
                .setMerchantData(ByteString.copyFrom(contractId.toString().getBytes()))
                .setSerializedRecurringPaymentDetails(recurringPaymentDetails.toByteString())
                .build();

        Protos.PaymentRequest result = Protos.PaymentRequest.newBuilder()
                .setPaymentDetailsVersion(1)
                .setPkiType("none")
                        //.setPkiData(null)
                .setSerializedPaymentDetails(details.toByteString())
                        //.setSignature(null)
                .build();


        result.writeTo(resp.getOutputStream());
        resp.setContentType("application/bitcoin-paymentrequest");
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private long localDateToMillis(LocalDate localDate) {
        return localDate.toDateTimeAtStartOfDay(DateTimeZone.UTC).getMillis();
    }

    private ByteString uuidToByteString(UUID id) throws UnsupportedEncodingException {
        return ByteString.copyFrom(id.toString(), "UTF-8");
    }

    private void pollForPayment(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException, CatalogApiException, PaymentApiException {
        final String contractIdString = req.getParameter("contractId");
        final String accountIdString = req.getParameter("accountId");
        Preconditions.checkNotNull(contractIdString);
        final UUID contractId = UUID.fromString(contractIdString);
        final String network = Objects.firstNonNull(req.getParameter("network"), "main");

        final List<PendingPayment> pendingPayments = paymentDao.getByBtcContractId(contractId);
        // TODO PIERRE combine multiple pending payments as long as this is within contract bounds.
        final PendingPayment pendingPayment = pendingPayments.size() > 0 ? pendingPayments.get(0) : null;

        final Payment payment = pendingPayment != null ? killbillAPI.getPaymentApi().getPayment(pendingPayment.getPaymentId(), false, createCallContext(req, resp)) : null;
        Preconditions.checkState(payment == null || payment.getCurrency() == Currency.BTC);

        final UUID accountId = UUID.fromString(accountIdString);
        transactionLogDao.insertTransactionLog(new TransactionLog(new DateTime(DateTimeZone.UTC), "pollForPayment", accountId, null, contractId));

        final long paymentAmountInSatochi = payment != null ? payment.getAmount().longValue() * BTC_TO_SATOSHIS : 0L;

        final DateTime now = new DateTime(DateTimeZone.UTC);

        final String memo = payment != null ? "Kill Bill payment " + payment.getId() : "No invoice to pay";

        final Protos.PaymentDetails.Builder detailsBuilder = Protos.PaymentDetails.newBuilder();
        detailsBuilder.setNetwork(network)
                .setTime(now.getMillis())
                .setExpires(now.plusDays(1).getMillis())
                .setMemo(memo)
                .setPaymentUrl(createURL(req, BTC_SUBSCRIPTION_PAYMENT_PATH))
                .setMerchantData(ByteString.copyFrom(contractId.toString().getBytes()));
        if (paymentAmountInSatochi > 0) {
            final Protos.Output.Builder outputBuilder = Protos.Output.newBuilder();
            outputBuilder.setAmount(paymentAmountInSatochi);
            final ECKey newPaymentKey = bitcoinManager.addKey();
            outputBuilder.setScript(ByteString.copyFrom(ScriptBuilder.createOutputScript(newPaymentKey).getProgram()));
            final Protos.Output output = outputBuilder.build();
            detailsBuilder.addOutputs(output);
        }
        final Protos.PaymentDetails details = detailsBuilder.build();

        final Protos.PaymentRequest result = Protos.PaymentRequest.newBuilder()
                .setPaymentDetailsVersion(1)
                .setPkiType("none")
                        //.setPkiData(null)
                .setSerializedPaymentDetails(details.toByteString())
                        //.setSignature(null)
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

        return req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + path + queryParamsBuilder.toString();
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

    private long getMaxPaymentAmount(@Nullable final Plan plan) throws CatalogApiException {
        if (plan == null) {
            return 0;
        }

        BigDecimal maxPaymentAmount = BigDecimal.ZERO;
        for (PlanPhase ph : plan.getAllPhases()) {
            BigDecimal phaseAmount = ph.getRecurringPrice() != null ? ph.getRecurringPrice().getPrice(Currency.BTC) : BigDecimal.ZERO;
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

    private UUID createSubscription(final Account account, final Plan plan, final String externalKey, final String priceList, final PhaseType phaseType, final DateTime now, final CallContext callContext) throws EntitlementApiException, TagApiException, CustomFieldApiException {

        final PlanPhaseSpecifier spec = new PlanPhaseSpecifier(plan.getProduct().getName(), plan.getProduct().getCategory(), plan.getBillingPeriod(), priceList, phaseType);
        final Entitlement entitlement = killbillAPI.getEntitlementApi().createBaseEntitlement(account.getId(), spec, externalKey, now.toLocalDate(), callContext);
        return entitlement.getId();
    }
}
