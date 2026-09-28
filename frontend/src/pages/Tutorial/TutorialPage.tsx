import './TutorialPage.css'

export function TutorialPage() {
  return (
    <div className="tutorial">
      <h1>Tutoriel</h1>
      <p>Un petit guide pour se repérer sur le site. Chaque section correspond à une page du menu.</p>

      <h2>Musiques</h2>
      <p>
        La liste de toutes les musiques de la base, avec leur franchise et leur jeu. Une barre de recherche
        filtre par franchise, jeu ou musique, et un menu déroulant permet de ne garder que celles qui ont
        une source (KHInsider ou YouTube) ou au contraire aucune.
      </p>
      <p>
        Le bouton ▶ à côté d'une musique ne s'affiche que si elle est vraiment jouable dans le navigateur
        (avoir une source ne suffit pas toujours). L'interrupteur « Lecture aléatoire » change ce que fait
        le bouton « suivant » du lecteur en bas de page : dans l'ordre, ou au hasard parmi les musiques
        actuellement affichées.
      </p>

      <h2>Blindtests</h2>
      <p>
        Avant de jouer ou de créer un blindtest, connecte-toi avec ton pseudo (bandeau en haut à droite) —
        cherche-le, ou crée-le s'il n'existe pas encore. Aucun mot de passe, c'est juste pour te reconnaître
        et garder ton score.
      </p>
      <p>
        Un blindtest est une liste de musiques fixée à sa création (nombre de musiques, difficulté, tags...).
        Une fois lancé, chaque musique se joue à son tour : cherche le jeu dans la liste proposée (jamais de
        texte à taper au hasard). Si tu es bloqué, un bouton permet de valider juste la franchise pour un
        point partiel. Le nombre d'essais est limité ; une fois épuisé, la musique est révélée. Le classement
        est un tableau trié par défaut sur les bonnes réponses, mais chaque colonne peut être cliquée pour
        trier autrement.
      </p>

      <h2>Ma culture</h2>
      <p>
        Deux façons de dire ce que tu connais ou non, en dehors de tout blindtest :
      </p>
      <p>
        <strong>Découvrir</strong> te propose une musique que tu n'as jamais votée, au hasard. Un
        interrupteur affiche ou cache le nom (caché par défaut) : caché, tu peux chercher le jeu ou répondre
        directement « Je connais »/« Je ne sais pas » ; affiché, deux boutons Oui/Non suffisent.
      </p>
      <p>
        <strong>Liste</strong> affiche toutes les musiques avec un Oui/Non par ligne, déjà rempli avec ce que
        tu as voté. Cliquer sur un bouton l'enregistre tout de suite ; recliquer dessus annule le vote.
      </p>

      <h2>Enrichissement</h2>
      <p>
        Cette page sert à compléter la base : ajouter des franchises, des jeux, des musiques, ou des liens
        KHInsider/YouTube à une musique qui n'en a pas encore. Une fois qu'un lien existe, il ne peut plus
        être changé depuis cette page (ça évite les erreurs) — seule son absence peut être comblée.
      </p>
      <p>
        L'arbre franchise → jeu → musique affiche des compteurs pour repérer d'un coup d'œil ce qui manque.
        Les filtres au-dessus permettent de n'afficher, par exemple, que les franchises sans jeu, les jeux
        avec peu ou pas de musiques, ou les musiques sans aucun lien.
      </p>
    </div>
  )
}
