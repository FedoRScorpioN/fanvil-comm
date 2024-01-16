CREATE TABLE IF NOT EXISTS mac_ip_pairs (
    mac_address VARCHAR(255) PRIMARY KEY,
    ip_address VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS computers (
    computer_name VARCHAR(255) PRIMARY KEY,
    mac_address VARCHAR(255) NOT NULL
);