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

import java.util.List;

public interface BitcoinConfig {

    @Config("org.killbill.bitcoin.block.confidence.depth")
    @Default("6")
    public int getConfidenceBlockDepth();

    @Config("org.killbill.bitcoin.install.dir")
    @Default(".")
    public String getInstallDirectory();

    @Config("org.killbill.bitcoin.install.dir")
    @Default("testnet")
    public String getNetworkName();

    @Config("org.killbill.bitcoin.plugins")
    public List<String> getKillbillBitcoinPlugins();
}

