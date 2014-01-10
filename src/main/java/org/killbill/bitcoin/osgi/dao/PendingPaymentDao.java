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

package org.killbill.bitcoin.osgi.dao;

import org.killbill.bitcoin.osgi.PendingPayment;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionStatus;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

public class PendingPaymentDao {

    private final DBI dbi;
    private final PendingPaymentMapper paymentMapper;

    public PendingPaymentDao(final DataSource dataSource) {
        this.dbi = new DBI(dataSource);
        this.paymentMapper = new PendingPaymentMapper();
    }

    public void insertPendingPayment(final PendingPayment payment) {
        dbi.inTransaction(new TransactionCallback<Void>() {
            @Override
            public Void inTransaction(Handle h, TransactionStatus status) throws Exception {
                h.createStatement("insert into btc_pending_payments (payment_id, account_id, tenant_id, btc_tx) VALUES (:payment_id, :account_id, :tenant_id, :btc_tx)")
                        .bind("payment_id", payment.getPaymentId().toString())
                        .bind("account_id", payment.getAccountId().toString())
                        .bind("tenant_id", payment.getTenantId().toString())
                        .bind("btc_tx", payment.getBtcTxHash())
                        .execute();
                return null;
            }
        });
    }

    public PendingPayment getByBtcTransactionId(final String btcTxHash) {
        return dbi.inTransaction(new TransactionCallback<PendingPayment>() {

            @Override
            public PendingPayment inTransaction(Handle h, TransactionStatus status) throws Exception {
                return h.createQuery("select payment_id, account_id, tenant_id, btc_tx from btc_pending_payments where btc_tx = :btc_tx")
                        .bind("btc_tx", btcTxHash)
                        .map(paymentMapper)
                        .first();
            }
        });
    }

    public List<PendingPayment> getAllPendingPayments() {
        return dbi.inTransaction(new TransactionCallback<List<PendingPayment>>() {

            @Override
            public List<PendingPayment> inTransaction(Handle h, TransactionStatus status) throws Exception {

                return h.createQuery("select payment_id, account_id, tenant_id, btc_tx from btc_pending_payments")
                        .map(paymentMapper)
                        .list();
            }
        });
    }

    public void removePendingPayment(final UUID paymentId) {
        dbi.inTransaction(new TransactionCallback<Void>() {
            @Override
            public Void inTransaction(Handle h, TransactionStatus status) throws Exception {
                h.createStatement("delete from btc_pending_payments where payment_id = :payment_id")
                        .bind("payment_id", paymentId.toString())
                        .execute();
                return null;
            }
        });

    }
}
