# Blindtests

Ce document décrit le fonctionnement des blindtests : leur structure, le déroulement d'une partie, et comment le score est calculé.

## Un blindtest est une recette partagée

Un blindtest est une liste fixe et ordonnée de musiques, créée une fois. Plusieurs personnes peuvent le jouer, chacune de son côté et à son propre rythme, mais toutes affrontent exactement les mêmes musiques dans le même ordre.

## Génération (première version)

À la création, on choisit :
- un nom ;
- un nombre de musiques ;
- une difficulté visée (curseur de 0 à 100), qui pioche parmi les musiques dont la proportion de votes « connaît » dans `knowledge` correspond à peu près à ce niveau.

Pour l'instant, la génération pioche dans **toute la base**. Restreindre à une franchise ou un jeu précis, ou choisir les musiques à la main, viendront plus tard.

## Déroulement d'une partie

Les musiques sont jouées dans l'ordre fixé à la création. Pour chacune :

1. La lecture démarre : la musique compte comme **écoutée** à partir de ce moment, que le joueur réponde ou non.
2. Le joueur tape le nom du jeu dans un champ de texte et valide, autant de fois qu'il veut, jusqu'à trouver la bonne réponse ou choisir de passer.
3. S'il tape le nom exact du jeu, c'est une bonne réponse.
4. S'il clique « Je passe », la réponse est révélée (franchise, jeu, titre), avec un bouton « J'aurais dû l'avoir » pour le cas où il a juste mal écrit le nom.
5. Un bouton « Suivant » permet de passer à la musique suivante.

Dans tous les cas (bonne réponse ou passe), la musique compte dans le total écouté. Un « je passe » sans cliquer « Ah, je connais en fait » vaut un vote « je ne connais pas » : il écrit dans `knowledge` comme une vraie réponse négative, pas comme une abstention. Cliquer « Ah, je connais en fait » marque la musique comme connue dans `knowledge`, comme une correction, mais ne compte pas comme une bonne réponse pour le score du blindtest : le score ne récompense que les réponses réellement tapées correctement.

La comparaison se fait aujourd'hui sur le nom exact du jeu : une abréviation ou une variante correcte du nom sera refusée. Un système de « noms valides » (réponses alternatives acceptées par musique) est prévu pour régler ça plus tard, voir « Ce qui n'est pas encore fait ».

## Score

Chaque réponse met à jour deux choses :

- `knowledge` (listener, musique) : comme pour l'import du Google Sheet, ça permet à un blindtest de continuer à enrichir la base au fil des parties.
- `blindtest_score` (blindtest, joueur) : deux compteurs, `good_answers` et `tracks_heard`. Pas de détail musique par musique pour l'instant, seulement les totaux.

Le classement d'un blindtest est trié par nombre de bonnes réponses, avec une bascule possible vers le pourcentage (`good_answers / tracks_heard`). Deux joueurs peuvent être à des stades différents de la partie : le classement est une photo de l'avancement de chacun à l'instant où on le consulte, pas une partie synchronisée.

## Rejouer un blindtest

Un joueur peut rejouer un blindtest déjà terminé, mais il n'y a qu'un seul score par (blindtest, joueur) : rejouer recommence à zéro et remplace le score précédent, il n'y a pas d'historique des tentatives.

## Identité

Pas de vrai compte pour l'instant. Un bandeau permet de rechercher son pseudo parmi les `listener` déjà en base (recherche approximative, pour retrouver le sien même mal orthographié) et de s'y « connecter ». En créer un nouveau utilise la même recherche, pour éviter les quasi-doublons (« Nyx » / « NyxCormo »). Accès libre, sans mot de passe : à sécuriser plus tard.

## Ce qui n'est pas encore fait

- Sélection manuelle des musiques d'un blindtest.
- Filtrer la génération par franchise ou par jeu.
- Blindtests basés sur des tags (le système de tags lui-même n'existe pas encore).
- Sécurisation de l'identité (mot de passe ou équivalent).
- Panneau d'administration.
- Mode « écouter les musiques sur lesquelles je n'ai pas encore voté », indépendant des blindtests, pour compléter `knowledge` plus vite.
- Système de noms valides pour accepter les abréviations et variantes de noms de jeu (déjà prévu pour l'import du Google Sheet, utile aussi ici).
