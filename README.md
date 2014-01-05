killbill-bitcoin-plugin
================================

Bitcoin plugin for Kill Bill


The plugin receives both the notifications from the Bitcoin network (blockchain, transactions, ...) and the one from Kill Bill, and it is in charge
to notify the payment sub system for when payments/refunds have been confirmed-- their matching bitcoin transaction has been inserted into a block
and there is enough confidence that block is on the longest chain.

To build, run `mvn clean install`. You can then install the plugin (`target/killbill-bitcoin-*.jar`) in `/var/tmp/bundles/platform`.
