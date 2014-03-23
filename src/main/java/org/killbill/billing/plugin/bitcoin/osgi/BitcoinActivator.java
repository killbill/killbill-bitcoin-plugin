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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;

import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.plugin.bitcoin.osgi.dao.ContractDao;
import org.killbill.billing.plugin.bitcoin.osgi.dao.PendingPaymentDao;
import org.killbill.billing.plugin.bitcoin.osgi.dao.TransactionLogDao;
import org.killbill.billing.plugin.bitcoin.osgi.http.PaymentRequestServlet;
import org.killbill.billing.plugin.bitcoin.osgi.payment.BitcoinPaymentPluginApi;
import org.killbill.killbill.osgi.libs.killbill.KillbillActivatorBase;
import org.killbill.killbill.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIKillbillEventHandler;
import org.osgi.framework.BundleContext;
import org.skife.config.ConfigurationObjectFactory;

public class BitcoinActivator extends KillbillActivatorBase {

    public static final String PLUGIN_NAME = "killbill-bitcoin";

    private OSGIKillbillEventHandler eventListener;
    private TransactionManager transactionManager;
    private Thread asyncInit;
    private BitcoinManager btcListener;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        final BitcoinConfig config = readBitcoinConfig();

        final ContractDao contractDao = new ContractDao(dataSource.getDataSource());
        final PendingPaymentDao paymentDao = new PendingPaymentDao(dataSource.getDataSource());
        final TransactionLogDao transactionLogDao = new TransactionLogDao(dataSource.getDataSource());

        this.transactionManager = new TransactionManager(logService, killbillAPI, paymentDao, config);

        // Register the handler to receive KB events
        this.eventListener = new KillbillListener(logService, killbillAPI, transactionManager, config);
        dispatcher.registerEventHandler(eventListener);

        // Register the payment plugin API
        registerPaymentPluginApi(context, new BitcoinPaymentPluginApi(killbillAPI));

        // Starts thread that will initialize btc library-- fetch latest blocks
        this.btcListener = new BitcoinManager(transactionManager, config);
        this.asyncInit = new Thread(new RunnableInit());
        asyncInit.start();

        final PaymentRequestServlet paymentRequestServlet = new PaymentRequestServlet(killbillAPI, contractDao, paymentDao, transactionLogDao, btcListener);
        registerServlet(context, paymentRequestServlet);
    }

    private BitcoinConfig readBitcoinConfig() {
        final Properties props = System.getProperties();
        final ConfigurationObjectFactory factory = new ConfigurationObjectFactory(props);
        return factory.build(BitcoinConfig.class);
    }

    private class RunnableInit implements Runnable {

        @Override
        public void run() {
            btcListener.start();
        }
    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        super.stop(context);
        btcListener.stop();
    }

    @Override
    public OSGIKillbillEventHandler getOSGIKillbillEventHandler() {
        return eventListener;
    }

    private void registerPaymentPluginApi(final BundleContext context, final PaymentPluginApi api) {
        final Dictionary props = new Hashtable();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, PaymentPluginApi.class, api, props);
    }

    private void registerServlet(final BundleContext context, final HttpServlet servlet) {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, PLUGIN_NAME);
        registrar.registerService(context, Servlet.class, servlet, props);
    }
}
