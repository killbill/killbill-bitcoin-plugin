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

package org.killbill.bitcoin.osgi;

import org.joda.time.DateTime;

import java.util.UUID;

public class TransactionLog {

    public final Long recordId;
    public final DateTime createdDate;
    public final String call;
    public final UUID accountId;
    public final UUID subscriptionId;
    public final UUID contractId;

    public TransactionLog(final Long recordId, final DateTime createdDate, final String call, final UUID accountId, final UUID subscriptionId, final UUID contractId) {
        this.recordId = recordId;
        this.createdDate = createdDate;
        this.call = call;
        this.accountId = accountId;
        this.subscriptionId = subscriptionId;
        this.contractId = contractId;
    }

    public TransactionLog(final DateTime createdDate, final String call, final UUID accountId, final UUID subscriptionId, final UUID contractId) {
        this(-1L, createdDate, call, accountId, subscriptionId, contractId);
    }

    public Long getRecordId() {
        return recordId;
    }

    public DateTime getCreatedDate() {
        return createdDate;
    }

    public String getCall() {
        return call;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public UUID getContractId() {
        return contractId;
    }
}
