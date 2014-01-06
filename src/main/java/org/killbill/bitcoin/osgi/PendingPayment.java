/*
 * Copyright 2010-2013 Ning, Inc.
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

package org.killbill.bitcoin.osgi;

import com.google.bitcoin.core.Sha256Hash;

import java.util.UUID;

public class PendingPayment {

    private final UUID paymentId;
    private final UUID accountId;
    private final UUID tenantId;
    private final Sha256Hash btcTxHash;

    public PendingPayment(final UUID paymentId, final UUID accountId, final UUID tenantId, final Sha256Hash btcTxHash) {
        this.paymentId = paymentId;
        this.accountId = accountId;
        this.tenantId = tenantId;
        this.btcTxHash= btcTxHash;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public Sha256Hash getBtcTxHash() {
        return btcTxHash;
    }
}
