package org.killbill.bitcoin.osgi.dao;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.killbill.bitcoin.osgi.PendingPayment;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

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

        final PendingPayment p1 = new PendingPayment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "12345");
        dao.insertPendingPayment(p1);

        final PendingPayment p1Get = dao.getByBtcTransactionId(p1.getBtcTxHash());
        assertEquals(p1Get, p1);

        final PendingPayment p2 = new PendingPayment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "23456");
        dao.insertPendingPayment(p2);

        final PendingPayment p2Get = dao.getByBtcTransactionId(p2.getBtcTxHash());
        assertEquals(p2Get, p2);

        final PendingPayment p3 = new PendingPayment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "34567");
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
