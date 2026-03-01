CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    account_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    branding_color_code VARCHAR(30)
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    full_name VARCHAR(255),
    role VARCHAR(50) DEFAULT 'USER',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    password VARCHAR(255),
    username VARCHAR(255) UNIQUE,
    account_id BIGINT,
    CONSTRAINT fk_user_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE SET NULL
);

CREATE INDEX idx_users_account_id ON users(account_id);

CREATE TABLE entries (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    party_size INTEGER NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    called BOOLEAN NOT NULL DEFAULT FALSE
);
