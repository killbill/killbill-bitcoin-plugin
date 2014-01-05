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
import org.osgi.framework.BundleContext;

public class BitcoinActivator extends KillbillActivatorBase {

    public static final String PLUGIN_NAME = "killbill-bitcoin";

    private OSGIKillbillEventHandler eventListener;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        // Register an event listener (optional)
        eventListener = new BitcoinListener(logService, killbillAPI);
        dispatcher.registerEventHandler(eventListener);

    }

    @Override
    public void stop(final BundleContext context) throws Exception {
        super.stop(context);

        // Do additional work on shutdown (optional)
    }

    @Override
    public OSGIKillbillEventHandler getOSGIKillbillEventHandler() {
        return eventListener;
    }
}
