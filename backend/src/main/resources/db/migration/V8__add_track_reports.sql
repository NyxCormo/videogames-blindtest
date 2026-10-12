-- Signalements : un votant indique que le son joué ne correspond pas au nom de la musique

create table track_report (
    track_id integer not null references track (id),
    listener_id integer not null references listener (id),
    created_at datetime not null,
    primary key (track_id, listener_id)
);
