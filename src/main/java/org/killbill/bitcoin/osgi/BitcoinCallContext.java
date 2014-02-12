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

import com.ning.billing.util.callcontext.CallContext;
import com.ning.billing.util.callcontext.CallOrigin;
import com.ning.billing.util.callcontext.UserType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.UUID;

public class BitcoinCallContext extends BitcoinTenantContext implements CallContext {

    private final DateTime now;
    private final String reasonCode;
    private final String comments;

    public BitcoinCallContext(final UUID tenantId, int confidenceDepth) {
        super(tenantId);
        this.now = new DateTime(DateTimeZone.UTC);
        this.reasonCode = "Chain validation";
        this.comments = "Bitcoin confidence: " + confidenceDepth;
    }

    public BitcoinCallContext(final UUID tenantId, String reasonCode, String comments) {
        super(tenantId);
        this.now = new DateTime(DateTimeZone.UTC);
        this.reasonCode = reasonCode;
        this.comments = comments;
    }

    @Override
    public UUID getUserToken() {
        return UUID.randomUUID();
    }

    @Override
    public String getUserName() {
        return BitcoinActivator.PLUGIN_NAME;
    }

    @Override
    public CallOrigin getCallOrigin() {
        return CallOrigin.EXTERNAL;
    }

    @Override
    public UserType getUserType() {
        return UserType.SYSTEM;
    }

    @Override
    public String getReasonCode() {
        return reasonCode;
    }

    @Override
    public String getComments() {
        return comments;
    }

    @Override
    public DateTime getCreatedDate() {
        return now;
    }

    @Override
    public DateTime getUpdatedDate() {
        return now;
    }
}
