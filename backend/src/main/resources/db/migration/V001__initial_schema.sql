create table users (
    id uuid primary key,
    preferred_weight_unit varchar(10) not null
);

create table workout_programs (
    id uuid primary key,
    user_id uuid not null,
    name varchar(120) not null,
    status varchar(20) not null,
    created_at timestamp with time zone not null,
    completed_at timestamp with time zone
);
create index idx_workout_programs_user_id on workout_programs(user_id);

create table program_sessions (
    id uuid primary key,
    program_id uuid not null references workout_programs(id) on delete cascade,
    sequence_number integer not null,
    name varchar(120) not null,
    is_completed boolean not null default false,
    unique (program_id, sequence_number)
);
create index idx_program_sessions_program_id on program_sessions(program_id);

create table program_exercise_targets (
    id uuid primary key,
    program_session_id uuid not null references program_sessions(id) on delete cascade,
    exercise_name varchar(120) not null,
    exercise_type varchar(20) not null,
    target_sets integer,
    target_reps integer,
    target_weight numeric(10,2),
    target_weight_unit varchar(10),
    target_duration_seconds integer,
    target_distance numeric(10,2),
    target_distance_unit varchar(10),
    sort_order integer not null
);
create index idx_program_exercise_targets_session_id on program_exercise_targets(program_session_id);

create table logged_sessions (
    id uuid primary key,
    user_id uuid not null,
    session_type varchar(20) not null,
    program_session_id uuid,
    session_date date not null,
    name varchar(120),
    notes varchar(2000),
    total_duration_seconds integer,
    created_at timestamp with time zone not null
);
create index idx_logged_sessions_user_date on logged_sessions(user_id, session_date desc);
create index idx_logged_sessions_program_session_id on logged_sessions(program_session_id);

create table session_feelings (
    session_id uuid primary key references logged_sessions(id) on delete cascade,
    rating integer not null,
    comment varchar(1000)
);

create table exercises (
    id uuid primary key,
    name varchar(120) not null unique,
    category varchar(120) not null,
    type varchar(20) not null,
    description varchar(2000),
    is_active boolean not null
);

create table exercise_entries (
    id uuid primary key,
    logged_session_id uuid not null references logged_sessions(id) on delete cascade,
    exercise_id uuid,
    custom_exercise_name varchar(120),
    exercise_name_snapshot varchar(120) not null,
    exercise_type varchar(20) not null,
    sort_order integer not null
);
create index idx_exercise_entries_session_id on exercise_entries(logged_session_id);
create index idx_exercise_entries_name_snapshot on exercise_entries(exercise_name_snapshot);

create table strength_sets (
    id uuid primary key,
    exercise_entry_id uuid not null references exercise_entries(id) on delete cascade,
    set_order integer not null,
    reps integer not null,
    weight_value numeric(10,2),
    weight_unit varchar(10),
    is_body_weight boolean not null,
    duration_seconds integer,
    rest_seconds integer
);
create index idx_strength_sets_entry_id on strength_sets(exercise_entry_id);

create table cardio_laps (
    id uuid primary key,
    exercise_entry_id uuid not null references exercise_entries(id) on delete cascade,
    lap_order integer not null,
    duration_seconds integer not null,
    distance_value numeric(10,2),
    distance_unit varchar(10)
);
create index idx_cardio_laps_entry_id on cardio_laps(exercise_entry_id);

