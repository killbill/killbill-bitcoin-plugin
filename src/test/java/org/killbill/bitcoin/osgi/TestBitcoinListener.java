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

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Wallet;
import org.skife.config.TimeSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

/*
* Manual test that verify we receive callbacks for transactions that apply to 'this' wallet.
*
* TEST 1 and PRE STEPS:
* Step 1: starts downloading the testnet blockchain
* Step 2: Add 1 keys to wallet
* Step 3: Request a payment transaction from faucet: http://tpfaucet.appspot.com/ to the public address
* -> That should trigger a payment.
*
* TEST 2;
* Step 1: Based on TEST 1, also create a new key and add to wallet.
* Step 2: Make a payment from previous key to new one.
*/

//

// Original key n3BNjWy6JgdhBofhZM3ZWkrH1SjdUsr6kX on testnet3, received 7.2 BTC from faucet
// New key mqUGjwEqngjxoAxnnoKz8HNP8UDVSKBWZ7 that will receive payments
//
// Key from faucet on which funds should be returned (eventually) : mmhmMNfBiZZ37g1tgg2t8DDbNoEdqKVxAL


public class TestBitcoinListener {

    private final static Logger logger = LoggerFactory.getLogger(TestBitcoinListener.class);

    private final static String DEFAULT_INSTALL_DIR = ".";
    private final static String NETWORK = "testnet";

    private final BitcoinManager bitcoinListener;
    private final BitcoinConfig config;

    public TestBitcoinListener(final BitcoinConfig config) {
        this.config = config;
        this.bitcoinListener = new BitcoinManager(new MockTransactionmanager(), config);
    }

    public void initializeBitcoinListener() {
        bitcoinListener.start();
    }

    public void addKeyToWallet() {
        final ECKey newKey = new ECKey();
        bitcoinListener.getKit().wallet().addKey(newKey);
        final NetworkParameters params = bitcoinListener.getNetworkParameters();
        logger.info("Added new key " + newKey.toAddress(params));
    }

    public void makePaymentTo() {
        // Add new key
        final ECKey newKey = new ECKey();
        bitcoinListener.getKit().wallet().addKey(newKey);
        final Wallet.SendRequest req = Wallet.SendRequest.to(bitcoinListener.getNetworkParameters(), newKey, BigInteger.valueOf(1000000));

        final NetworkParameters params = bitcoinListener.getNetworkParameters();
        logger.info("Added new key to receive payment " + newKey.toAddress(params));

        bitcoinListener.getKit().wallet().sendCoins(req);
    }

    public static void main(String[] args) {

        final BitcoinConfig config = new BitcoinConfig() {
            @Override
            public boolean shouldGenerateKey() {
                return false;
            }

            @Override
            public int getConfidenceBlockDepth() {
                return 1;
            }

            @Override
            public String getInstallDirectory() {
                return DEFAULT_INSTALL_DIR;
            }

            @Override
            public String getNetworkName() {
                return NETWORK;
            }

            @Override
            public List<String> getKillbillBitcoinPlugins() {
                return Collections.singletonList("killbill-coinbase");
            }

            @Override
            public String getForwardBankHash() {
                return null;
            }

            @Override
            public Long getMinForwardBalance() {
                return null;
            }

            @Override
            public TimeSpan getForwardBankInterval() {
                return null;
            }

        };
        final TestBitcoinListener test = new TestBitcoinListener(config);
        test.initializeBitcoinListener();
        test.makePaymentTo();
    }

    public static class MockTransactionmanager extends TransactionManager {

        public MockTransactionmanager() {
            super(null, null, null, null);
        }

        @Override
        public boolean notifyPaymentSystemIfExists(final String hash) {
            logger.info("Received confirmed transcation " + hash);
            return true;
        }
    }
}
