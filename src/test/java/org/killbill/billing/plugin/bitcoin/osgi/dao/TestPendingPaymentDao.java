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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.killbill.billing.plugin.bitcoin.osgi.PendingPayment;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import static org.testng.Assert.assertEquals;

public class TestPendingPaymentDao {

    private DataSource dataSource;
    private PendingPaymentDao dao;

    @BeforeSuite(groups = "sql")
    public void setup() {
        dataSource = createDataSource();
        dao = new PendingPaymentDao(dataSource);
    }

    @BeforeTest(groups = "sql")
    public void setuptest() throws SQLException {
        cleanupTable();
    }

    @Test(groups = "sql")
    public void testbasic() {
        final String bitcoinTransactionId = UUID.randomUUID().toString();

        final PendingPayment p1 = new PendingPayment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), bitcoinTransactionId, "12345");
        dao.insertPendingPayment(p1);

        final PendingPayment p1Get = dao.getByBtcTransactionId(p1.getBtcTxHash());
        assertEquals(p1Get, p1);

        final PendingPayment p2 = new PendingPayment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), bitcoinTransactionId, "23456");
        dao.insertPendingPayment(p2);

        final PendingPayment p2Get = dao.getByBtcTransactionId(p2.getBtcTxHash());
        assertEquals(p2Get, p2);

        final PendingPayment p3 = new PendingPayment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), bitcoinTransactionId, "34567");
        dao.insertPendingPayment(p3);

        final List<PendingPayment> all1 = dao.getAllPendingPayments();
        assertEquals(all1.size(), 3);
        assertFoundPayment(p1, all1);
        assertFoundPayment(p2, all1);
        assertFoundPayment(p3, all1);

        dao.removePendingPayment(p2.getPaymentId());

        final List<PendingPayment> all2 = dao.getAllPendingPayments();
        assertEquals(all2.size(), 2);
        assertFoundPayment(p1, all1);
        assertFoundPayment(p3, all1);

    }

    private void assertFoundPayment(final PendingPayment input, final List<PendingPayment> all) {
        for (PendingPayment cur : all) {
            if (cur.equals(input)) {
                return;
            }
        }
        Assert.fail("Failed to find payment " + input);
    }

    private void cleanupTable() throws SQLException {
        final Connection conn = dataSource.getConnection();
        final PreparedStatement st = conn.prepareStatement("truncate table btc_pending_payments");
        try {
            st.execute();
        } finally {
            st.close();
            conn.close();
        }
    }

    private DataSource createDataSource() {
        final MysqlDataSource ds = new MysqlDataSource();
        ds.setServerName("localhost");
        ds.setPortNumber(3306);
        ds.setDatabaseName("killbill");
        ds.setUser("root");
        ds.setPassword("root");
        return ds;
    }
}
