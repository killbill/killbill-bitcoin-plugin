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

import java.util.UUID;

public class PendingPayment {

    private final UUID paymentId;
    private final UUID accountId;
    private final UUID tenantId;
    private final String btcTxHash;

    public PendingPayment(final UUID paymentId, final UUID accountId, final UUID tenantId, final String btcTxHash) {
        this.paymentId = paymentId;
        this.accountId = accountId;
        this.tenantId = tenantId;
        this.btcTxHash = btcTxHash;
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

    public String getBtcTxHash() {
        return btcTxHash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PendingPayment)) return false;

        PendingPayment that = (PendingPayment) o;

        if (accountId != null ? !accountId.equals(that.accountId) : that.accountId != null) return false;
        if (btcTxHash != null ? !btcTxHash.equals(that.btcTxHash) : that.btcTxHash != null) return false;
        if (paymentId != null ? !paymentId.equals(that.paymentId) : that.paymentId != null) return false;
        if (tenantId != null ? !tenantId.equals(that.tenantId) : that.tenantId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = paymentId != null ? paymentId.hashCode() : 0;
        result = 31 * result + (accountId != null ? accountId.hashCode() : 0);
        result = 31 * result + (tenantId != null ? tenantId.hashCode() : 0);
        result = 31 * result + (btcTxHash != null ? btcTxHash.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PendingPayment{" +
                "paymentId=" + paymentId +
                ", accountId=" + accountId +
                ", tenantId=" + tenantId +
                ", btcTxHash='" + btcTxHash + '\'' +
                '}';
    }
}
