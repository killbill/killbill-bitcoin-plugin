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

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

public class BankForwarder {


    private final static Logger log = LoggerFactory.getLogger(BankForwarder.class);

    private final static long BTC_TO_SATOSHI = (100L * 1000L * 1000L);

    private volatile boolean isRunning;
    private volatile boolean isShuttingDown;

    private final Thread forwarder;
    private final String bankHash;

    public BankForwarder(final BitcoinConfig config, final Wallet wallet, final NetworkParameters params) {

        this.isRunning = false;
        this.isShuttingDown = false;
        this.bankHash = config.getForwardBankHash();

        // Create forwarder thread if there is some target bank to forward the money to
        this.forwarder = this.bankHash != null ? new Thread(new Runnable() {

            private long nextForwardTime = System.currentTimeMillis();

            @Override
            public void run() {

                isRunning = true;

                log.info("Starting bank forwarder thread");

                synchronized (forwarder) {
                    do {
                        if (isShuttingDown) {
                            break;
                        }

                        long remaingSleepTime = nextForwardTime - System.currentTimeMillis();
                        if (remaingSleepTime <= 0) {
                            doTransfer();
                            nextForwardTime = System.currentTimeMillis() + config.getForwardBankInterval().getMillis();
                        } else {
                            try {
                                forwarder.wait(remaingSleepTime);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    } while (!isShuttingDown);

                    // reset running and notify stop thread.
                    isRunning = false;

                    log.info("Stopping bank forwarder thread...");

                    synchronized (forwarder) {
                        forwarder.notify();
                    }
                }
            }

            private void doTransfer() {
                try {
                    final BigInteger balance = wallet.getBalance();
                    log.info("Current wallet current balance = " + balance + " satoshis ( ~ " + (balance.doubleValue() / BTC_TO_SATOSHI) + " BTC) ");

                    if (balance.longValue() < config.getMinForwardBalance()) {
                        return;
                    }

                    final Address output = new Address(params, bankHash);
                    final Wallet.SendRequest req = Wallet.SendRequest.emptyWallet(output);
                    final Wallet.SendResult result = wallet.sendCoins(req);
                    log.info("Emptying wallet txHash = " + result.tx.getHash() + ",  tx = " + result.tx.toString());
                } catch (AddressFormatException e) {
                    log.warn("Failed to empty wallet to target address " + bankHash, e);
                }
            }
        }) : null;

    }

    public void start() {
        if (!isRunning && forwarder != null) {
            isShuttingDown = false;
            forwarder.start();
        }
    }

    public void stop() {

        if (!isRunning) {
            return;
        }

        // Notify forwarder we want to stop
        synchronized (forwarder) {
            isShuttingDown = true;
            forwarder.notify();
        }

        // Wait for forwarder to complete stop
        do {
            synchronized (forwarder) {
                try {
                    forwarder.wait(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } while (isRunning);
    }
}
