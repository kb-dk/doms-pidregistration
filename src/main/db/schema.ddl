CREATE TYPE job_state AS ENUM ('pending', 'done', 'error');
CREATE TABLE job (
  id                SERIAL PRIMARY KEY UNIQUE,
  uuid              VARCHAR(41) NOT NULL UNIQUE,
  collection        VARCHAR(20) NOT NULL,
  state             job_state   NOT NULL,
  last_state_change TIMESTAMP   NOT NULL
);
CREATE INDEX job_uuid_index ON job (uuid);
CREATE INDEX job_collection_index ON job (collection);
CREATE INDEX job_last_state_change_index ON job (last_state_change);