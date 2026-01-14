-- H2 stubs for pgcrypto
CREATE ALIAS IF NOT EXISTS PGP_SYM_DECRYPT FOR "org.karnak.backend.h2.PgCryptoNoop.pgpSymDecrypt";
CREATE ALIAS IF NOT EXISTS PGP_SYM_ENCRYPT FOR "org.karnak.backend.h2.PgCryptoNoop.pgpSymEncrypt";
CREATE ALIAS IF NOT EXISTS CURRENT_SETTING FOR "org.karnak.backend.h2.PgCryptoNoop.currentSetting";
