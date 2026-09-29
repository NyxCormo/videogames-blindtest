import './TutorialPage.css'

export function TutorialPage() {
  return (
    <div className="tutorial">
      <h1>Tutoriel</h1>
      <p>
        Ce guide présente les différentes pages du site, dans l'ordre du menu.
        Si jamais vous avez des recommandations de trucs à modifier je ne demande que ça.
      </p>

      <h2>Musiques</h2>
      <p>
        Cette page liste toutes les musiques de la base avec leur franchise et leur jeu. La barre de recherche
        filtre par franchise, par jeu ou par musique. Le menu déroulant permet de n'afficher que les musiques
        qui ont une source (KHInsider ou YouTube), ou celles qui n'en ont pas.
      </p>
      <p>
        Le bouton de lecture n'apparaît que pour les musiques réellement jouables dans le navigateur, car avoir
        une source ne suffit pas toujours. Le bouton « Lecture aléatoire » change le comportement du bouton
        « suivant » du lecteur en bas de page. Désactivé, il passe à la musique suivante de la liste. Activé, il
        en choisit une au hasard parmi celles affichées.
      </p>
      <p>
        Un clic sur le nom d'une musique ouvre sa page, où l'on peut ajouter ou retirer des tags (genre,
        ambiance, plateforme...). Les tags de genre et de plateforme peuvent aussi être appliqués à toutes les
        musiques du jeu en une seule fois.
      </p>

      <h2>Blindtests</h2>
      <p>
        Pour jouer ou créer un blindtest, il faut d'abord se connecter avec un pseudo grâce au bouton en haut à
        droite. Il suffit de chercher son pseudo, ou de le créer s'il n'existe pas encore. Il n'y a pas de mot
        de passe, le pseudo sert seulement à enregistrer les scores.
      </p>
      <p>
        Les musiques d'un blindtest sont choisies une fois pour toutes à sa création, selon le nombre de
        musiques, la difficulté et les tags demandés. Pendant la partie, les musiques passent une par une. Pour
        répondre, il faut choisir le jeu dans la liste proposée. En cas de doute, un bouton permet de ne donner
        que la franchise, ce qui rapporte un point partiel. Le nombre d'essais par musique est limité, et la
        réponse est révélée une fois les essais épuisés.
      </p>
      <p>
        Le classement est trié par défaut sur le nombre de bonnes réponses. Un clic sur l'en-tête d'une colonne
        trie le tableau selon cette colonne.
      </p>

      <h2>Ma culture (si vous avez un meilleur nom je suis preneur)</h2>
      <p>Cette partie sert à indiquer quelles musiques on connaît, en dehors des blindtests. Elle propose deux pages.</p>
      <p>
        <strong>Découvrir</strong> fait écouter, au hasard, une musique pour laquelle on n'a pas encore voté. Le
        nom de la musique est caché par défaut. Dans ce cas, on peut chercher le jeu ou répondre directement
        avec « Je connais » ou « Je ne sais pas ». Si le nom est affiché, il suffit de répondre par Oui ou par
        Non.
      </p>
      <p>
        <strong>Liste</strong> affiche toutes les musiques avec un bouton Oui et un bouton Non sur chaque ligne,
        déjà remplis avec les votes existants. Un clic enregistre le vote immédiatement, et un second clic sur
        le même bouton l'annule.
      </p>

      <h2>Enrichissement</h2>
      <p>
        Cette page sert à compléter la base en ajoutant des franchises, des jeux, des musiques, ou des liens
        KHInsider et YouTube aux musiques qui n'en ont pas. Un lien déjà enregistré ne peut pas être modifié
        depuis cette page, pour éviter les erreurs. Il est aussi impossible d'ajouter un élément qui existe
        déjà.
      </p>
      <p>
        La base y est présentée sous forme d'arbre (franchises, puis jeux, puis musiques), avec des compteurs
        qui montrent ce qui manque. Les filtres permettent par exemple de n'afficher que les franchises sans
        jeu, les jeux avec peu de musiques ou les musiques sans lien.
      </p>

      <h2>Exporter en CSV</h2>
      <p>
        Ce lien du menu télécharge toute la base dans un fichier au même format que le Google Sheet d'origine,
        avec une colonne par votant.
      </p>
    </div>
  )
}
