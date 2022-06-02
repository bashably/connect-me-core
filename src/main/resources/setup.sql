CREATE TABLE user (
    username VARCHAR(20) PRIMARY KEY,
    password_hash VARCHAR(64) NOT NULL, -- SHA256 Hash
    phone_number VARCHAR(15) NOT NULL UNIQUE, -- phone number must be unique
    auth_token TEXT, -- JWT Token session key
    current_location POINT, -- geographic location (latitude, longitude)
    created_on TIMESTAMP NOT NULL,
    last_update_on TIMESTAMP NOT NULL
);

CREATE TABLE interest_root ( -- represents a single interest
    id SERIAL PRIMARY KEY, -- id of interest
    created_on TIMESTAMP NOT NULL,
    last_update_on TIMESTAMP NOT NULL
);

CREATE TABLE interest_term ( -- represent the interest root with in different terms or languages
    id SERIAL PRIMARY KEY, -- id language term for interest
    interest_id BIGINT UNSIGNED NOT NULL, -- actual reference to root interest
    term VARCHAR(30) NOT NULL, -- term for interest in language
    lang VARCHAR(2) NOT NULL, -- language of term
    CONSTRAINT interest_lang_unique UNIQUE (interest_id, lang, term),
    CONSTRAINT FOREIGN KEY (interest_id) REFERENCES interest_root(id) ON DELETE CASCADE
);