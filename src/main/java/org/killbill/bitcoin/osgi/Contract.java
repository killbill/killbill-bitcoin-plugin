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

import org.joda.time.LocalDate;

import java.util.UUID;

public class Contract {
    private final BitcoinSubscriptionId bitcoinSubscriptionId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final UUID contractId;

    public Contract(BitcoinSubscriptionId bitcoinSubscriptionId, LocalDate startDate, LocalDate endDate, UUID contractId) {
        this.bitcoinSubscriptionId = bitcoinSubscriptionId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.contractId = contractId;
    }

    public BitcoinSubscriptionId getBitcoinSubscriptionId() {
        return bitcoinSubscriptionId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public UUID getContractId() {
        return contractId;
    }
}
