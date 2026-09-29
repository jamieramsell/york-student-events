CREATE TABLE student_event_subscriptions (
    id uuid PRIMARY KEY,
    user_id uuid REFERENCES users (id) NOT NULL,
    event_id uuid REFERENCES event (id) NOT NULL,
    source varchar NOT NULL
);