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

package org.killbill.billing.plugin.bitcoin.osgi;

import java.util.UUID;

public class PendingPayment {

    private final Integer recordId;
    private final UUID paymentId;
    private final UUID accountId;
    private final UUID tenantId;
    private final String btcTxHash;
    private final String btcContractId;

    public PendingPayment(final UUID paymentId, final UUID accountId, final UUID tenantId, String btcTxHash, final String btcContractId) {
        this(-1, paymentId, accountId, tenantId, btcTxHash, btcContractId);
    }

    public PendingPayment(final Integer recordId, final UUID paymentId, final UUID accountId, final UUID tenantId, String btcTxHash, final String btcContractId) {
        this.recordId = recordId;
        this.paymentId = paymentId;
        this.accountId = accountId;
        this.tenantId = tenantId;
        this.btcTxHash = btcTxHash;
        this.btcContractId = btcContractId;
    }

    public Integer getRecordId() {
        return recordId;
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

    public String getBtcContractId() {
        return btcContractId;
    }

    @Override
    public String toString() {
        return "PendingPayment{" +
               "recordId=" + recordId +
               ", paymentId=" + paymentId +
               ", accountId=" + accountId +
               ", tenantId=" + tenantId +
               ", btcTxHash='" + btcTxHash + '\'' +
               ", btcContractId='" + btcContractId + '\'' +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PendingPayment)) {
            return false;
        }

        PendingPayment that = (PendingPayment) o;

        if (accountId != null ? !accountId.equals(that.accountId) : that.accountId != null) {
            return false;
        }
        if (btcContractId != null ? !btcContractId.equals(that.btcContractId) : that.btcContractId != null) {
            return false;
        }
        if (btcTxHash != null ? !btcTxHash.equals(that.btcTxHash) : that.btcTxHash != null) {
            return false;
        }
        if (paymentId != null ? !paymentId.equals(that.paymentId) : that.paymentId != null) {
            return false;
        }
        if (recordId != null ? !recordId.equals(that.recordId) : that.recordId != null) {
            return false;
        }
        if (tenantId != null ? !tenantId.equals(that.tenantId) : that.tenantId != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = recordId != null ? recordId.hashCode() : 0;
        result = 31 * result + (paymentId != null ? paymentId.hashCode() : 0);
        result = 31 * result + (accountId != null ? accountId.hashCode() : 0);
        result = 31 * result + (tenantId != null ? tenantId.hashCode() : 0);
        result = 31 * result + (btcTxHash != null ? btcTxHash.hashCode() : 0);
        result = 31 * result + (btcContractId != null ? btcContractId.hashCode() : 0);
        return result;
    }
}
