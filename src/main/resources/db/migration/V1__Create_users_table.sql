CREATE TABLE users (
  id VARCHAR(36) NOT NULL UNIQUE,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  email_verification_code VARCHAR(36) UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  status VARCHAR(255) NOT NULL,
  signed_up TIMESTAMP NOT NULL
);

