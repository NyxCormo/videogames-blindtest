-- Résultat de la recherche de pochettes sur le CDN, album par album, pour ne pas refaire les mêmes requêtes

create table album_scan (
    album_url text primary key,
    scanned_at datetime not null
);

create table album_scan_image (
    album_url text not null references album_scan (album_url),
    position integer not null,
    image_url text not null,
    primary key (album_url, position)
);
