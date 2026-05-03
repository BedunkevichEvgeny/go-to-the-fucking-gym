CREATE TABLE session_ai_suggestions (
    session_id   UUID        NOT NULL,
    suggestion   TEXT        NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_session_ai_suggestions PRIMARY KEY (session_id),
    CONSTRAINT fk_session_ai_suggestions_session
        FOREIGN KEY (session_id) REFERENCES logged_sessions (id)
);

