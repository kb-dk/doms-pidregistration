CREATE TYPE job_state AS ENUM ('pending', 'done', 'error');
CREATE TABLE jobs (
  id                SERIAL PRIMARY KEY UNIQUE,
  uuid              VARCHAR(41) NOT NULL UNIQUE,
  collection        VARCHAR(20) NOT NULL,
  state             job_state   NOT NULL,
  created           TIMESTAMP   NOT NULL,
  last_state_change TIMESTAMP   NOT NULL
);
CREATE INDEX jobs_uuid_index ON jobs (uuid);
CREATE INDEX jobs_collection_index ON jobs (collection);
CREATE INDEX jobs_created_index ON jobs (created);
CREATE INDEX jobs_last_state_change_index ON jobs (last_state_change);