/*! SET storage_engine=INNODB */;

DROP TABLE IF EXISTS btc_pending_payments;
CREATE TABLE btc_pending_payments (
    payment_id char(36) NOT NULL,
    account_id char(36) NOT NULL,
    tenant_id char(36) NOT NULL,
    btc_tx varchar(128) NOT NULL,
    PRIMARY KEY(payment_id)
) CHARACTER SET utf8 COLLATE utf8_bin;
CREATE UNIQUE INDEX pending_payments_btc_tx ON btc_pending_payments(btc_tx);


