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

package org.killbill.billing.plugin.bitcoin.osgi.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.killbill.billing.plugin.bitcoin.osgi.PendingPayment;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

public class PendingPaymentMapper implements ResultSetMapper<PendingPayment> {

    @Override
    public PendingPayment map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        final Integer recordId = r.getInt("record_id");
        final UUID paymentId = UUID.fromString(r.getString("payment_id"));
        final UUID accountId = UUID.fromString(r.getString("account_id"));
        final UUID tenantId = r.getString("tenant_id") != null ? UUID.fromString(r.getString("tenant_id")) : null;
        final String btcTxHash = r.getString("btc_tx");
        final String btcContractId = r.getString("btc_contract_id");
        return new PendingPayment(recordId, paymentId, accountId, tenantId, btcTxHash, btcContractId);
    }
}
