create table blindtest_difficulty_band (
    blindtest_id integer not null references blindtest (id),
    position integer not null,
    min_difficulty integer not null,
    max_difficulty integer not null,
    proportion integer not null,
    primary key (blindtest_id, position)
);
