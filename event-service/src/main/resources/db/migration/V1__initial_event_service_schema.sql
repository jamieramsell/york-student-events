CREATE TABLE cohort (
    id uuid PRIMARY KEY, 
    name varchar NOT NULL,
    department varchar NOT NULL,
    academic_year integer NOT NULL,
    year_group integer NOT NULL
);

CREATE TABLE users (
    id uuid PRIMARY KEY,
    user_type varchar NOT NULL,
    username varchar NOT NULL,
    email varchar NOT NULL,
    cohort uuid REFERENCES cohort (id) -- todo: remove Cohort attribute from User
);

CREATE TABLE cohort_members (
    cohort_id uuid REFERENCES cohort (id),
    member_id uuid REFERENCES users (id),
    PRIMARY KEY (cohort_id, member_id)
);

CREATE TABLE venue (
    id uuid PRIMARY KEY,
    name varchar NOT NULL,
    address varchar NOT NULL,
    capacity integer
);

CREATE TABLE event (
    id uuid PRIMARY KEY,
    title varchar NOT NULL,
    description varchar,
    start_date_time timestamp,
    end_date_time timestamp,
    location varchar,
    capacity integer,
    category varchar NOT NULL
);

CREATE TABLE user_events (
    user_id uuid REFERENCES users (id),
    event_id uuid REFERENCES event (id),
    PRIMARY KEY (user_id, event_id)
);