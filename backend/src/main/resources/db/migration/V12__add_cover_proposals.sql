-- Propositions d'images des joueurs (une adresse, ou rien pour signaler seulement), qui remplacent le simple signalement

alter table game drop column cover_reported_at;

create table cover_proposal (
    game_id integer not null references game (id),
    listener_id integer not null references listener (id),
    image_url text,
    created_at datetime not null,
    primary key (game_id, listener_id)
);
