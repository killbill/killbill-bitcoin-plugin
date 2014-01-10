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

import org.skife.config.Config;
import org.skife.config.Default;
import org.skife.config.Description;
import org.skife.config.TimeSpan;

import java.util.List;

public interface BitcoinConfig {

    @Description("Whether to add a new key in that wallet")
    @Config("org.killbill.bitcoin.generate.key")
    @Default("false")
    public boolean shouldGenerateKey();

    @Description("Depth of the transaction -- number of blocks mined created on or after that transaction was mined")
    @Config("org.killbill.bitcoin.block.confidence.depth")
    @Default("6")
    public int getConfidenceBlockDepth();

    @Description("Installation directlry for storing block chain and wallet")
    @Config("org.killbill.bitcoin.install.dir")
    @Default(".")
    public String getInstallDirectory();

    @Description("Bitcoin chain network")
    @Config("org.killbill.bitcoin.network")
    @Default("testnet")
    public String getNetworkName();

    @Description("Comma separated list of plugins registered in Kill Bill that deal with BTC transactions")
    @Config("org.killbill.bitcoin.plugins")
    public List<String> getKillbillBitcoinPlugins();

    @Description("The public hash for the bank on which wallet funds should be forwarded to ")
    @Config("org.killbill.bitcoin.forward.bank")
    public String getForwardBankHash();

    @Description("The time interval at which funds will be forwarded to the bank")
    @Config("org.killbill.bitcoin.forward.interval")
    @Default("1h")
    public TimeSpan getForwardBankInterval();
}

