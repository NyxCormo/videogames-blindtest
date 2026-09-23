-- Ajout des blindtests (docs/blindtests.md)

create table blindtest (
    id integer primary key,
    name text not null,
    difficulty integer not null,
    created_at datetime not null
);

create table blindtest_track (
    blindtest_id integer not null references blindtest (id),
    track_id integer not null references track (id),
    position integer not null,
    primary key (blindtest_id, position)
);

create table blindtest_score (
    blindtest_id integer not null references blindtest (id),
    listener_id integer not null references listener (id),
    good_answers integer not null,
    tracks_heard integer not null,
    primary key (blindtest_id, listener_id)
);
