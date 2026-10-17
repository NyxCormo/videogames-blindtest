-- Dates de création et de dernière visite des pseudos, inconnues pour les pseudos déjà existants

alter table listener add column created_at datetime;
alter table listener add column last_seen_at datetime;
