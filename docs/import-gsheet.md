# Import du Google Sheet

Ce document décrit comment les données du Google Sheet d'InsaLan sont importées dans la base.
Il s'appuie sur [`ost-insalan-base.csv`](ost-insalan-base.csv), un export du Google Sheet du 21/09/26.

## Format

Fichier **CSV** exporté depuis Google Sheets : `Fichier > Télécharger > Valeurs séparées par des virgules` (feuille `BDD`). Encodage UTF-8, séparateur virgule, fin de ligne CRLF, 33 colonnes (A à AG) dont 24 colonnes de votants.

Un export depuis Excel en français est souvent en point-virgule et en ANSI : il n'est pas pris en charge.

## Structure du fichier

- Les lignes 1 à 3 contiennent le titre et les consignes : elles sont ignorées.
- La ligne 4 contient les en-têtes, les données commencent à la ligne 5.
- Les colonnes sont lues **par position** et non par nom : les en-têtes contiennent des compteurs qui changent à chaque édition (par exemple « Licence, nb: 315 »).

## Correspondance des colonnes

| Colonne | En-tête | Destination | Remarque |
|---|---|---|---|
| A | Licence | `franchise.name` | |
| B | Jeu | `game.name` | rattaché à la franchise de la ligne |
| C | OST | `track.name` | rattaché au jeu de la ligne |
| D | Noms Valides | non importé | à faire à la toute fin du projet |
| E | Lien page web | `track.khinsider_link` | |
| F | (vide) | ignorée | |
| G | Lien audio | `track.youtube_link` si le lien est YouTube, sinon ignoré | voir « Liens audio » |
| H, I | Nb de vote, Vote moyenne | non importés | calculés par formule, l'application les recalcule |
| J et suivantes | noms des votants | `listener.name` (en-tête) et `knowledge.knows` (cellules) | une colonne par votant |

`track.duration`, `track.preferred_start`, `track.audio_link` et `track.audio_link_resolved_at` ne sont pas renseignés par l'import.

## Liens audio

Aucun lien de la colonne « Lien audio » n'est fiable : ils expirent ou disparaissent.

- Un lien **YouTube** (`youtube.com` ou `youtu.be`) est repris dans `track.youtube_link`.
- Tout autre lien (`jukehost`, `vgmtreasurechest`, `archive.org`...) est ignoré.
- `audio_link` et `audio_link_resolved_at` sont remplis uniquement par l'application, à partir de la page KHInsider de la musique (`khinsider_link`), au moment où le lien est demandé.

## Votes

Le nom d'un votant est celui de l'en-tête de sa colonne. Dans le fichier versionné, ces noms sont remplacés par `UserA`, `UserB`... (anonymisation).

Une cellule vaut `1` (sait reconnaître la musique), `0` (ne sait pas) ou est vide (pas de vote).
Une cellule vide, ou qui ne contient qu'un espace, ne crée aucune ligne dans `knowledge`.

Un votant est créé au premier vote de sa part rencontré (un votant sans aucun vote n'est pas créé). Sur un import relancé, un vote existant prend la valeur du Sheet (si vote supprimé, rien ne se passe).

## Règles de nettoyage

- Les espaces en début et en fin de cellule sont retirés.
- Les noms sont comparés **tels quels**, casse comprise : `Spider-Man` et `Spider-man` donnent deux franchises distinctes (4 cas dans le fichier). Une correction viendra plus tard, avec du code dédié.
- Un jeu est identifié par le couple (franchise, nom) : `Origins` existe à la fois sous Assassin's Creed et sous Rayman.
- Une musique est identifiée par le couple (jeu, nom).
- Un jeu ou une musique qui porte le même nom que sa franchise ou son jeu (par exemple Stellar Blade / Stellar Blade) est normal : aucun traitement particulier.
- L'import peut être relancé sans créer de doublons. Pour un musique déjà en base, les liens KHInsider et YouTube sont mis à jour si la cellule est remplie (une cellule vide n'efface rien).
- L'import se fait en une seule transaction, s'il y a une erreur, rien ne se passe.


## Lignes sans titre de musique

Elles sont **conservées** : une ligne qui n'a que la licence crée la franchise, une ligne qui a la licence et le jeu crée aussi le jeu, sans aucune musique. Cela permet de suggérer des jeux avant d'avoir trouvé leur bande originale, et plus tard de lister tous les jeux avec leur nombre de musiques pour savoir où chercher (voir la roadmap du [README](../README.md)).

Une ligne sans licence, sans jeu et sans titre est ignorée. Les votes d'une ligne sans titre ne peuvent être rattachés à aucune musique : ils sont ignorés. Une ligne avec un jeu mais sans licence, ou avec un titre mais sans jeu, ne peut pas être rattachée et est donc ignorée et comptabilisée dans le rapport d'import.

## Le fichier au 21/09/26

| | |
|---|---|
| Lignes de données (à partir de la ligne 5) | 1405 |
| dont musiques complètes (licence, jeu et titre) | 1296 |
| dont licence et jeu sans titre | 14 |
| dont licence seule | 15 |
| dont lignes vides (aucune des trois colonnes) | 80 |
| Votants | 24 |
| Lignes avec « Noms Valides » | 131 |

## Résultat attendu de l'import

Ces chiffres servent de référence pour vérifier l'import.

| | |
|---|---|
| Franchises | 313 (dont 15 sans jeu) |
| Jeux | 535 (dont 14 sans musique) |
| Musiques | 1296 |
| Musiques avec un lien KHInsider | 194 |
| Musiques avec un lien YouTube | 19 |
| Votants (`listener`) | 24 |
| Votes (`knowledge`) | 8207 (dont 3052 « connaît ») |

Ignorés : 80 lignes vides, 878 liens audio non YouTube, 21 votes situés sur 11 lignes sans titre.
