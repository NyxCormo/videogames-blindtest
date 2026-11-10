-- Audio des musiques YouTube téléchargé sur le serveur : nom du fichier, date du téléchargement, dernière erreur

alter table track add column youtube_audio_file text;
alter table track add column youtube_audio_downloaded_at datetime;
alter table track add column youtube_audio_error text;
