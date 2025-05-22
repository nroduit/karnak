-- Create void functions used for encryption in the PostgreSQL database otherwise the Spring Boot test context does not load
DROP ALIAS IF EXISTS PGP_SYM_DECRYPT;
CREATE ALIAS PGP_SYM_DECRYPT AS 'String decrypt(String value, String key){ return value; }';
DROP ALIAS IF EXISTS PGP_SYM_ENCRYPT;
CREATE ALIAS PGP_SYM_ENCRYPT AS 'String encrypt(String value, String key){ return value; }';
DROP ALIAS IF EXISTS CURRENT_SETTING;
CREATE ALIAS CURRENT_SETTING AS 'String currentSetting(String value) { return null; }';