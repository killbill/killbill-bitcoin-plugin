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

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.bitcoin.core.AbstractWalletEventListener;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.RegTestParams;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.utils.BriefLogFormatter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.protobuf.ByteString;

public class BitcoinManager {

    private static final Logger log = LoggerFactory.getLogger(BitcoinManager.class);

    private final WalletAppKit kit;
    private final TransactionManager transactionManager;
    private final BitcoinConfig config;

    private BankForwarder forwarder;

    private volatile boolean isInitialized;

    public BitcoinManager(final TransactionManager transactionManager, final BitcoinConfig config) {
        this.transactionManager = transactionManager;
        this.config = config;
        this.kit = initializeKit();
        this.isInitialized = false;
    }

    public void start() {
        // Download the block chain and wait until it's done.
        kit.startAndWait();

        addKeyIfMissing();

        startBankForwarder();

        log.info(walletAsString());

        kit.wallet().addEventListener(new AbstractWalletEventListener() {
            @Override
            public void onTransactionConfidenceChanged(Wallet wallet, Transaction tx) {

                final String txHashStr = tx.getHash().toString();

                log.info("Bitcoin listener received new transaction " + txHashStr + ", confidence = " + tx.getConfidence());

                if (tx.getConfidence().getDepthInBlocks() < config.getConfidenceBlockDepth()) {
                    return;
                }

                final boolean success = transactionManager.notifyPaymentSystemIfExists(txHashStr);
                if (success) {
                    log.info("Bitcoin notifing transaction manager for " + tx.getHash() + ", confidence = " + tx.getConfidence());
                }
            }
        });
        this.isInitialized = true;
    }

    private void startBankForwarder() {
        this.forwarder = new BankForwarder(config, kit.wallet(), getNetworkParameters());
        forwarder.start();
    }

    public void stop() {
        forwarder.stop();
    }

    public void commitTransaction(Transaction tx) {
        kit.wallet().maybeCommitTx(tx);
    }

    public ECKey addKey() {
        final ECKey newKey = new ECKey();
        kit.wallet().addKey(newKey);
        log.info("GENERATED NEW KEY FOR BITCOIN WALLET : " + newKey.toAddress(getNetworkParameters()));
        return newKey;
    }

    public void addKeyIfMissing() {
        if (config.shouldGenerateKey()) {
            addKey();
        }
    }

    public String walletAsString() {
        return kit.wallet().toString(false, true, true, null);
    }

    public Collection<TransactionOutput> isMine(final ByteString transactionBytes) {

        Transaction transaction = new Transaction(getNetworkParameters(), transactionBytes.toByteArray());
        return Collections2.<TransactionOutput>filter(transaction.getOutputs(), new Predicate<TransactionOutput>() {
            @Override
            public boolean apply(TransactionOutput output) {
                return output.isMine(kit.wallet());
            }
        });
    }

    public Transaction broadcastTransaction(final ByteString transactionBytes) throws ExecutionException, InterruptedException {
        final Transaction tx = new Transaction(getNetworkParameters(), transactionBytes.toByteArray());
        kit.peerGroup().broadcastTransaction(tx);
        return tx;
    }

    private WalletAppKit initializeKit() {

        BriefLogFormatter.init();

        final NetworkParameters params = getNetworkParameters();
        final String filePrefix = getFilePrefix();

        // Start up a basic app using a class that automates some boilerplate.
        final WalletAppKit tmpKit = new WalletAppKit(params, new File(config.getInstallDirectory()), filePrefix);
        tmpKit.setAutoSave(true);
        tmpKit.setUserAgent("killbill", "1.0");

        if (params == RegTestParams.get()) {
            // Regression test mode is designed for testing and development only, so there's no public network for it.
            // If you pick this mode, you're expected to be running a local "bitcoind -regtest" instance.
            tmpKit.connectToLocalHost();
        }
        return tmpKit;
    }

    public List<String> getKeys() {
        final List<String> keys = new LinkedList<String>();
        for (final ECKey ecKey : kit.wallet().getKeys()) {
            // TODO PIERRE
            keys.add(ecKey.toAddress(getNetworkParameters()).toString());
        }
        return keys;
    }

    @VisibleForTesting
    WalletAppKit getKit() {
        return kit;
    }

    @VisibleForTesting
    NetworkParameters getNetworkParameters() {
        NetworkParameters params;
        if (config.getNetworkName().equals("testnet")) {
            params = TestNet3Params.get();
        } else if (config.getNetworkName().equals("regtest")) {
            params = RegTestParams.get();
        } else {
            params = MainNetParams.get();
        }
        return params;
    }

    private String getFilePrefix() {
        if (config.getNetworkName().equals("testnet")) {
            return "killbill-bitcoin-testnet";
        } else if (config.getNetworkName().equals("regtest")) {
            return "killbill-bitcoin-regtest";
        } else {
            return "killbill-bitcoin";
        }
    }
}
