create table profile_goal_onboarding_attempts (
    id uuid primary key,
    user_id uuid not null references users(id) on delete cascade,
    age integer not null,
    current_weight numeric(10,2) not null,
    weight_unit varchar(10) not null,
    primary_goal varchar(40) not null,
    goal_target_bucket varchar(20),
    status varchar(20) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);
create index idx_onboarding_attempts_user_status on profile_goal_onboarding_attempts(user_id, status);

create table plan_proposals (
    id uuid primary key,
    attempt_id uuid not null references profile_goal_onboarding_attempts(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    version integer not null,
    status varchar(20) not null,
    proposal_payload text not null,
    provider varchar(40) not null,
    model_deployment varchar(120) not null,
    created_at timestamp with time zone not null,
    unique (attempt_id, version)
);
create index idx_plan_proposals_attempt_id on plan_proposals(attempt_id);
create index idx_plan_proposals_user_id on plan_proposals(user_id);

create table proposal_feedback (
    id uuid primary key,
    attempt_id uuid not null references profile_goal_onboarding_attempts(id) on delete cascade,
    proposal_id uuid not null unique references plan_proposals(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    requested_changes varchar(2000) not null,
    created_at timestamp with time zone not null
);
create index idx_proposal_feedback_attempt_id on proposal_feedback(attempt_id);
create index idx_proposal_feedback_user_id on proposal_feedback(user_id);

create table accepted_program_activations (
    id uuid primary key,
    attempt_id uuid not null unique references profile_goal_onboarding_attempts(id) on delete cascade,
    proposal_id uuid not null unique references plan_proposals(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    activated_program_id uuid not null references workout_programs(id),
    replaced_program_id uuid references workout_programs(id),
    activated_at timestamp with time zone not null
);
create index idx_accepted_program_activations_user_id on accepted_program_activations(user_id);

