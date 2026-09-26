# Blindtests

Ce document décrit le fonctionnement des blindtests : leur structure, le déroulement d'une partie, et comment le score est calculé.

## Un blindtest est une recette partagée

Un blindtest est une liste fixe et ordonnée de musiques, créée une fois. Plusieurs personnes peuvent le jouer, chacune de son côté et à son propre rythme, mais toutes affrontent exactement les mêmes musiques dans le même ordre.

## Génération

À la création, on choisit :
- un nom ;
- un nombre de musiques ;
- un ou plusieurs paliers de difficulté, chacun une fourchette (0 = musiques les plus connues, 100 = les moins connues, ajustable avec une barre à deux poignées) et une proportion du nombre total de musiques — les proportions doivent totaliser 100 % ;
- des plafonds optionnels : nombre maximum de musiques par jeu, et par franchise ;
- un nombre d'essais par musique, optionnel (5 par défaut) — voir « Déroulement d'une partie » ;
- une stratégie de sélection optionnelle (aléatoire, favoriser les jeux rares, favoriser les franchises rares) ; sans choix explicite, les trois sont essayées dans cet ordre jusqu'à ce que l'une réussisse à générer une sélection complète ;
- des tags optionnels pour restreindre les musiques éligibles (combinaison ET par défaut, ou OU).

Seules les musiques jouables (avec un `audio_link`) ayant au moins un vote dans `knowledge` sont éligibles. Le tirage est aléatoire dans chaque palier (pas juste les musiques les plus proches d'un point de difficulté), pour éviter que les mêmes musiques reviennent d'un blindtest à l'autre. Si aucune combinaison ne satisfait les critères (paliers, plafonds, tags trop stricts), la création échoue avec un message clair plutôt que de générer un blindtest incomplet.

Le `difficulty` affiché dans la liste des blindtests est une moyenne pondérée des paliers, uniquement pour l'affichage : le détail complet des paliers utilisés est gardé à part.

## Déroulement d'une partie

Les musiques sont jouées dans l'ordre fixé à la création. Pour chacune :

1. La lecture démarre.
2. Le joueur cherche le jeu dans une liste (case « Jeu », qui affiche chaque résultat sous la forme « Nom (Franchise) » et cherche aussi bien sur le nom du jeu que sur celui de la franchise) et valide — jamais de texte libre : on choisit un jeu qui existe réellement, ce qui évite les faux échecs dus à la casse, aux espaces, ou aux noms de jeux incohérents d'une entrée à l'autre dans le Sheet (parfois juste le nom du jeu, parfois licence + jeu).
3. En plus, optionnel, pour un point bonus : chercher la musique exacte parmi celles du jeu choisi.
4. En cas de blocage, un bouton « Je sais juste la franchise » propose de chercher parmi **toutes** les franchises, y compris celles qui n'ont aucun jeu (ajoutées exprès comme leurres, pour qu'un joueur ne puisse pas deviner au hasard qu'une franchise ne peut jamais être la bonne réponse). Une franchise correcte donne un point de franchise, sans révéler la musique ; le joueur peut ensuite chercher le jeu, désormais restreint aux jeux de cette franchise.
5. Chaque tentative (jeu ou franchise seule) consomme un essai, dans un quota commun fixé à la création. Le nombre d'essais restants est affiché en direct.
6. Quota épuisé sans avoir trouvé le jeu, ou clic sur « Je passe » : la réponse est révélée (franchise, jeu, titre), avec un bouton « Ah, je connais en fait » pour le cas où le joueur savait mais n'a pas trouvé à temps.
7. Un bouton « Suivant » permet de passer à la musique suivante.

La musique compte comme **écoutée** au moment où elle est résolue (bonne réponse, passe, ou essais épuisés) — pas au moment où la lecture démarre, pour qu'un rechargement de page en plein milieu d'une musique ne la fasse pas sauter au rechargement. Une musique non trouvée (passe ou essais épuisés) écrit `knowledge = false`, comme une vraie réponse négative, pas une abstention. Cliquer « Ah, je connais en fait » corrige `knowledge` à connu, mais ne change jamais le score du blindtest : celui-ci ne récompense que ce qui a été réellement trouvé pendant la partie.

## Score

Chaque musique peut rapporter jusqu'à trois types de points, indépendants les uns des autres :

- **Franchise** (`franchise_answers`, +1) : la franchise a été trouvée, que ce soit en trouvant directement le jeu ou par le chemin de secours. Compté une seule fois par musique, même si la franchise est validée puis le jeu trouvé ensuite.
- **Bonnes réponses** (`good_answers`, +1) : le jeu exact a été trouvé. Trouver le jeu directement donne aussi le point de franchise du même coup, puisque connaître le jeu implique de connaître sa franchise.
- **Bonus** (`bonus_answers`, +1) : en plus du jeu, la musique exacte a été trouvée. Ne compte pas dans le score principal : les noms de musiques varient trop selon qui les a saisies dans le Sheet (anglais, français, abrégé...) pour en faire une vraie question fiable — c'est juste une statistique supplémentaire au classement.

`blindtest_score` garde aussi `tracks_heard`, `total_attempts` (cumulé sur tout le blindtest, utile comme statistique) et deux compteurs de travail propres à la musique en cours (`attempts_used_on_current_track`, `franchise_found_on_current_track`), remis à zéro à chaque nouvelle musique.

Le classement d'un blindtest est un tableau. Une colonne « Rang » affiche un classement **officiel**, à départage en cascade : bonnes réponses, puis leur pourcentage, puis franchises trouvées, puis musiques bonus, puis ordre alphabétique. Les autres colonnes (bonnes réponses, franchise, bonus, musiques écoutées, avec leurs pourcentages) restent triables en cliquant leur en-tête pour explorer le classement autrement ; le rang de chaque joueur ne change pas quand on trie l'affichage par une autre colonne. Deux joueurs peuvent être à des stades différents de la partie : le classement est une photo de l'avancement de chacun à l'instant où on le consulte, pas une partie synchronisée.

## Rejouer un blindtest

Un joueur peut rejouer un blindtest déjà terminé, mais il n'y a qu'un seul score par (blindtest, joueur) : rejouer recommence à zéro et remplace le score précédent, il n'y a pas d'historique des tentatives.

## Identité

Pas de vrai compte pour l'instant. Un bandeau permet de rechercher son pseudo parmi les `listener` déjà en base (recherche approximative, pour retrouver le sien même mal orthographié) et de s'y « connecter ». En créer un nouveau utilise la même recherche, pour éviter les quasi-doublons (« Nyx » / « NyxCormo »). Accès libre, sans mot de passe : à sécuriser plus tard.

## Ce qui n'est pas encore fait

- Sélection manuelle des musiques d'un blindtest.
- Sécurisation de l'identité (mot de passe ou équivalent).
- Panneau d'administration.
- Mode « écouter les musiques sur lesquelles je n'ai pas encore voté », indépendant des blindtests, pour compléter `knowledge` plus vite.

Le système de « noms valides » (accepter les abréviations et variantes d'un nom de jeu) initialement prévu pour la comparaison de texte n'est plus nécessaire pour les blindtests : la réponse se fait maintenant par sélection dans une liste (jeu, franchise, musique bonus), plus jamais par texte libre.
