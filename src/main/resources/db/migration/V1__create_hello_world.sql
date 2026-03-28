CREATE TABLE hello_world (
    id   BIGSERIAL PRIMARY KEY,
    message VARCHAR(255) NOT NULL
);

INSERT INTO hello_world (message) VALUES ('Hello from Postgres!');
