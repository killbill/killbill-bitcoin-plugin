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

import com.google.protobuf.ByteString;
import com.ning.billing.ObjectType;

import java.io.Serializable;
import java.util.UUID;

public class BitcoinSubscriptionId implements Serializable {
    private final UUID entityId;
    private final ObjectType alignment;

    // TODO we should really return a UUID to the wallet
    public static BitcoinSubscriptionId fromString(String serialized) {
        String[] parts = serialized.split("::");
        return new BitcoinSubscriptionId(ObjectType.valueOf(parts[0]), UUID.fromString(parts[1]));
    }

    public BitcoinSubscriptionId(ObjectType alignment, UUID entityId) {
        this.entityId = entityId;
        this.alignment = alignment;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public ObjectType getAlignment() {
        return alignment;
    }

    public ByteString toByteString() {
        return ByteString.copyFromUtf8(toString());
    }

    @Override
    public String toString() {
        return String.format("%s::%s", alignment.name(), entityId.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BitcoinSubscriptionId)) return false;

        BitcoinSubscriptionId that = (BitcoinSubscriptionId) o;

        if (alignment != that.alignment) return false;
        if (entityId != null ? !entityId.equals(that.entityId) : that.entityId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = entityId != null ? entityId.hashCode() : 0;
        result = 31 * result + (alignment != null ? alignment.hashCode() : 0);
        return result;
    }
}
