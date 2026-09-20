-- Initial schema, following docs/database/schema.dbml

create table franchise (
    id integer primary key,
    name text not null
);

create table game (
    id integer primary key,
    name text not null,
    franchise_id integer not null references franchise (id)
);

create table track (
    id integer primary key,
    name text not null,
    game_id integer not null references game (id),
    khinsider_link text,
    youtube_link text,
    audio_link text,
    audio_link_resolved_at datetime,
    duration integer,
    preferred_start integer
);

create table listener (
    id integer primary key,
    name text not null
);

-- SQLite n'a pas de type booléen : 0 ou 1
create table knowledge (
    listener_id integer not null references listener (id),
    track_id integer not null references track (id),
    knows integer not null check (knows in (0, 1)),
    primary key (listener_id, track_id)
);

create table tag_type (
    id integer primary key,
    name text not null unique
);

create table tag (
    id integer primary key,
    name text not null,
    type_id integer not null references tag_type (id),
    unique (type_id, name)
);

create table track_tag (
    track_id integer not null references track (id),
    tag_id integer not null references tag (id),
    primary key (track_id, tag_id)
);
