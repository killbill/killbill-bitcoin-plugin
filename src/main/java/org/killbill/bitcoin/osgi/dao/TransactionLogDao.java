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

import org.killbill.bitcoin.osgi.TransactionLog;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionStatus;

import javax.sql.DataSource;

public class TransactionLogDao {

    private final DBI dbi;

    public TransactionLogDao(final DataSource dataSource) {
        this.dbi = new DBI(dataSource);
    }

    public void insertTransactionLog(final TransactionLog log) {
        dbi.inTransaction(new TransactionCallback<Void>() {
            @Override
            public Void inTransaction(Handle h, TransactionStatus status) throws Exception {
                h.createStatement("insert into transaction_logs (account_id, subscription_id, contract_id, api_call, created_date) VALUES (:account_id, :subscription_id, :contract_id, :api_call, :created_date)")
                        .bind("account_id", log.getAccountId().toString())
                        .bind("subscription_id", log.getSubscriptionId() != null ? log.getSubscriptionId().toString() : null)
                        .bind("contract_id", log.getContractId().toString())
                        .bind("api_call", log.getCall())
                        .bind("created_date", log.getCreatedDate().toDate())
                        .execute();
                return null;
            }
        });
    }
}
