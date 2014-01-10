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

import com.ning.killbill.osgi.libs.killbill.KillbillActivatorBase;
import com.ning.killbill.osgi.libs.killbill.OSGIKillbillEventDispatcher.OSGIKillbillEventHandler;
import org.killbill.bitcoin.osgi.dao.PendingPaymentDao;
import org.osgi.framework.BundleContext;
import org.skife.config.ConfigurationObjectFactory;

import java.util.Properties;

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

        final PendingPaymentDao paymentDao = new PendingPaymentDao(dataSource.getDataSource());
        this.transactionManager = new TransactionManager(logService, killbillAPI, paymentDao, config);

        // Register the handler to receive KB events
        this.eventListener = new KillbillListener(logService, killbillAPI, transactionManager, config);
        dispatcher.registerEventHandler(eventListener);

        // Starts thread that will initialize btc library-- fetch latest blocks
        this.btcListener = new BitcoinManager(transactionManager, config);
        this.asyncInit = new Thread(new RunnableInit());
        asyncInit.start();
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
}
