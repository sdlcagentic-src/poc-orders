-- Messages a consumer has already handled (shared foundation, idempotent consumer).
CREATE TABLE processed_message (
    consumer     VARCHAR(100)             NOT NULL,
    message_id   VARCHAR(255)             NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (consumer, message_id)
);
