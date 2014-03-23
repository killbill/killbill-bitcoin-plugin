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

import java.util.UUID;

import javax.sql.DataSource;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.killbill.billing.plugin.bitcoin.osgi.Contract;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionStatus;

public class ContractDao {

    private final DBI dbi;
    private final ContractMapper contractMapper;

    public ContractDao(final DataSource dataSource) {
        this.dbi = new DBI(dataSource);
        this.contractMapper = new ContractMapper();
    }

    public void insertContract(final Contract contract) {
        dbi.inTransaction(new TransactionCallback<Void>() {
            @Override
            public Void inTransaction(Handle h, TransactionStatus status) throws Exception {
                h.createStatement("insert into btc_contracts (entity_id, object_type, contract_id, start_date, end_date, created_date) VALUES (:entity_id, :object_type, :contract_id, :start_date, :end_date, :created_date)")
                 .bind("entity_id", contract.getBitcoinSubscriptionId().getEntityId().toString())
                 .bind("object_type", contract.getBitcoinSubscriptionId().getAlignment().name())
                 .bind("contract_id", contract.getContractId().toString())
                 .bind("start_date", contract.getStartDate().toDate())
                 .bind("endDate", contract.getEndDate() == null ? null : contract.getEndDate().toDate())
                 .bind("created_date", new DateTime(DateTimeZone.UTC).toDate())
                 .execute();
                return null;
            }
        });
    }

    public Contract getContract(final UUID contractId) {
        return dbi.inTransaction(new TransactionCallback<Contract>() {

            @Override
            public Contract inTransaction(Handle h, TransactionStatus status) throws Exception {
                return h.createQuery("select * from btc_contracts where contract_id = :contract_id")
                        .bind("contract_id", contractId.toString())
                        .map(contractMapper)
                        .first();
            }
        });
    }
}
