import type { ReactNode } from 'react'
import { Link } from 'react-router'

export type TutorialSection = { title: string; content: ReactNode }

export const TUTORIAL_SECTIONS: TutorialSection[] = [
  {
    title: 'Comment jouer ?',
    content: (
      <>
        <ol>
          <li>
            Appuyez sur le bouton <strong>Se connecter</strong> et tapez votre pseudo. Si vous n'avez
            pas encore de compte, appuyez ensuite sur <strong>« Créer [Votre pseudo] »</strong>.
          </li>
          <li>
            Ensuite, allez sur la page <Link to="/blindtests">Blindtests</Link> pour jouer à un
            blindtest déjà existant et affronter ceux qui ont déjà participé, ou{' '}
            <Link to="/blindtests/new">créez-en un</Link> !
          </li>
        </ol>
        <p>
          Si vous avez envie de découvrir toutes les musiques disponibles, et de nous aider à avoir un
          système de difficulté (basé sur vos scores), testez la page{' '}
          <Link to="/knowledge">Ma culture</Link>.
        </p>
        <p>
          Sinon, vous pouvez nous aider à mettre <strong>VOS</strong> musiques dans l'onglet{' '}
          <Link to="/tracks?tab=enrichir">Enrichir</Link> de la page Musiques (tuto juste après).
        </p>
      </>
    ),
  },
  {
    title: 'Comment mettre VOS musiques ?',
    content: (
      <>
        <p>
          Dans l'onglet <Link to="/tracks?tab=enrichir">Enrichir</Link> de la page Musiques, vous
          pouvez ajouter des licences (ex : Zelda, Pokémon, Assassin's Creed) avec le bouton en haut,
          et vous pouvez ajouter des jeux et des musiques.
        </p>
        <p>
          <strong>
            N'hésitez pas à ajouter des licences et des jeux même si vous ne mettez pas de musiques !!!
          </strong>{' '}
          Cela va pouvoir donner des idées pour les autres joueurs.
        </p>

        <h3>Ajouter une musique</h3>
        <p>
          Pour pouvoir ajouter une musique, vous devez vous rendre sur le site{' '}
          <a href="https://downloads.khinsider.com/" target="_blank" rel="noreferrer">
            downloads.khinsider.com
          </a>{' '}
          et chercher votre musique :
        </p>
        <ol>
          <li>Trouvez un album et cliquez dessus.</li>
          <li>Trouvez votre musique et cliquez dessus.</li>
          <li>Vous devriez être sur la bonne page !</li>
        </ol>
        <p>
          Regardez les liens de la page <Link to="/tracks">Musiques</Link> pour avoir un exemple de
          comment les liens marchent.
        </p>
      </>
    ),
  },
  {
    title: 'La musique ne correspond pas à son nom ?',
    content: (
      <>
        <p>
          Si le son joué n'est pas le bon, appuyez sur le bouton{' '}
          <strong>Signaler un mauvais son</strong> (il faut être connecté). Vous le trouverez à côté
          du lecteur de la page <Link to="/tracks">Musiques</Link>, sur la page{' '}
          <Link to="/knowledge/discover">Découvrir</Link> et dans les blindtests une fois la réponse
          affichée.
        </p>
        <p>
          Un bouton rouge veut dire que la musique a déjà été signalée. Vous pouvez la signaler vous
          aussi, ou appuyer à nouveau pour annuler votre signalement.
        </p>
      </>
    ),
  },
]
