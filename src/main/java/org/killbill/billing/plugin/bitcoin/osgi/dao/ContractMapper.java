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
import java.sql.Timestamp;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.killbill.billing.ObjectType;
import org.killbill.billing.plugin.bitcoin.osgi.BitcoinSubscriptionId;
import org.killbill.billing.plugin.bitcoin.osgi.Contract;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

public class ContractMapper implements ResultSetMapper<Contract> {

    @Override
    public Contract map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        final Integer recordId = r.getInt("record_id");
        final UUID entityId = UUID.fromString(r.getString("entity_id"));
        final ObjectType objectType = ObjectType.valueOf(r.getString("object_type"));
        final UUID contractId = UUID.fromString(r.getString("contract_id"));
        final LocalDate startDate = new LocalDate(r.getTimestamp("start_date"));
        final Timestamp endDateTimeStamp = r.getTimestamp("end_date");
        final LocalDate endDate = r.wasNull() ? null : new LocalDate(endDateTimeStamp);
        final DateTime createdDate = new DateTime(r.getTimestamp("created_date")).toDateTime(DateTimeZone.UTC);
        return new Contract(new BitcoinSubscriptionId(objectType, entityId), startDate, endDate, contractId);
    }
}

