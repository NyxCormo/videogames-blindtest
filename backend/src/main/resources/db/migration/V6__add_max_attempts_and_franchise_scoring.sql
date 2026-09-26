alter table blindtest add column max_attempts integer not null default 5;

alter table blindtest_score add column franchise_answers integer not null default 0;
alter table blindtest_score add column total_attempts integer not null default 0;
alter table blindtest_score add column attempts_used_on_current_track integer not null default 0;
