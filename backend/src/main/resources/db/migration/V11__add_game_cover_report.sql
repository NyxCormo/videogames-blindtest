-- Date du dernier signalement « Ce n'est pas la bonne image », vide quand rien n'est signalé

alter table game add column cover_reported_at datetime;
