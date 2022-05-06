CREATE TABLE user (
    username VARCHAR(20) PRIMARY KEY,
    password_hash VARCHAR(64) NOT NULL, -- SHA256 Hash
    phone_number VARCHAR(15) NOT NULL UNIQUE, -- phone number must be unique
    auth_token TEXT, -- JWT Token session key
    current_location POINT, -- geographic location (latitude, longitude)
    created_on TIMESTAMP NOT NULL, -- when was the user created?
    last_update_on TIMESTAMP NOT NULL -- when was the user updated the last time?
);