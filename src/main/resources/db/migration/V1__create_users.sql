CREATE TABLE app_user (
    username VARCHAR(50)  PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(20)  NOT NULL CHECK (role IN ('EMPLOYEE', 'APPROVER'))
);

INSERT INTO app_user (username, password, role) VALUES
    ('john.smith',    '$2b$12$U9nB0Lm4HRssu1GI/F96EOiXom0XA189.YPyVMjxtafxWbvuPrZG.', 'EMPLOYEE'),
    ('jane.doe',      '$2b$12$mdeebSamOg9vewZxzk4jYOOS/lFBWBo/yVS1YuZEUAi5TSAJ7un9q', 'EMPLOYEE'),
    ('mike.approver', '$2b$12$NEFY42D0Oj7FoZ.TRazfFu9jJDDnTzAzG6xpHcsQPJb1UoWpHtzo.', 'APPROVER');
