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

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.killbill.bitcoin.osgi.TransactionLog;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;

public class TransactionLogMapper implements ResultSetMapper<TransactionLog> {
    @Override
    public TransactionLog map(int index, ResultSet r, StatementContext ctx) throws SQLException {

        final Long recordId = r.getLong("record_id");
        final Timestamp resultStamp = r.getTimestamp("created_date");
        final DateTime createdDate = r.wasNull() ? null : new DateTime(resultStamp).toDateTime(DateTimeZone.UTC);
        final String call = r.getString("call");
        final UUID accountId = UUID.fromString(r.getString("account_id"));
        final UUID subscriptionId = r.getString("subscription_id") != null ? UUID.fromString(r.getString("subscription_id")) : null;
        final UUID contractId = UUID.fromString(r.getString("contract_id"));
        //final UUID tenantId = UUID.fromString(r.getString("tenant_id"));
        return new TransactionLog(recordId, createdDate, call, accountId, subscriptionId, contractId);
    }
}
