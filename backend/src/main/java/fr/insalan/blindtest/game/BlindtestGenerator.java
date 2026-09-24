package fr.insalan.blindtest.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import fr.insalan.blindtest.model.Blindtest;
import fr.insalan.blindtest.model.BlindtestTrack;
import fr.insalan.blindtest.model.Knowledge;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.BlindtestRepository;
import fr.insalan.blindtest.repository.BlindtestTrackRepository;
import fr.insalan.blindtest.repository.KnowledgeRepository;
import fr.insalan.blindtest.repository.TrackTagRepository;
import jakarta.transaction.Transactional;

// Génère un blindtest en piochant des musiques jouables dont le ratio de votes "connaît" se rapproche de la difficulté visée.
@Service
public class BlindtestGenerator {

    private final BlindtestRepository blindtestRepository;
    private final BlindtestTrackRepository blindtestTrackRepository;
    private final KnowledgeRepository knowledgeRepository;
    private final TrackTagRepository trackTagRepository;

    public BlindtestGenerator(
        BlindtestRepository blindtestRepository,
        BlindtestTrackRepository blindtestTrackRepository,
        KnowledgeRepository knowledgeRepository,
        TrackTagRepository trackTagRepository
    ) {
        this.blindtestRepository = blindtestRepository;
        this.blindtestTrackRepository = blindtestTrackRepository;
        this.knowledgeRepository = knowledgeRepository;
        this.trackTagRepository = trackTagRepository;
    }

    public Blindtest generate(String name, int trackCount, int difficulty) {
        return generate(name, trackCount, difficulty, List.of(), true);
    }

    @Transactional
    public Blindtest generate(String name, int trackCount, int difficulty, List<Integer> tagIds, boolean matchAllTags) {
        double targetRatio = (100 - difficulty) / 100.0;
        List<Track> candidates = closestToTarget(targetRatio, tagIds == null ? List.of() : tagIds, matchAllTags);

        if (candidates.size() < trackCount) {
            throw new IllegalStateException(
                "Pas assez de musiques jouables avec des votes : " + candidates.size() + " disponibles, " + trackCount + " demandées"
            );
        }

        List<Track> picked = new ArrayList<>(candidates.subList(0, trackCount));
        Collections.shuffle(picked);

        Blindtest blindtest = blindtestRepository.save(new Blindtest(name, difficulty));
        for (int position = 0; position < picked.size(); position++) {
            blindtestTrackRepository.save(new BlindtestTrack(blindtest, picked.get(position), position));
        }
        return blindtest;
    }

    // Musiques jouables ayant au moins un vote, triées de la plus proche à la plus éloignée du ratio ciblé.
    private List<Track> closestToTarget(double targetRatio, List<Integer> tagIds, boolean matchAllTags) {
        Map<Track, List<Knowledge>> votesByTrack = knowledgeRepository.findAll().stream()
            .filter(knowledge -> knowledge.getTrack().getAudioLink() != null)
            .collect(Collectors.groupingBy(Knowledge::getTrack));

        Map<Integer, Set<Integer>> tagIdsByTrackId = tagIds.isEmpty() ? Map.of() : tagIdsByTrackId();

        return votesByTrack.keySet().stream()
            .filter(track -> tagIds.isEmpty() || matchesTagFilter(tagIdsByTrackId.getOrDefault(track.getId(), Set.of()), tagIds, matchAllTags))
            .sorted(Comparator.comparingDouble(track -> Math.abs(knowsRatio(votesByTrack.get(track)) - targetRatio)))
            .toList();
    }

    private boolean matchesTagFilter(Set<Integer> trackTagIds, List<Integer> tagIds, boolean matchAllTags) {
        return matchAllTags ? trackTagIds.containsAll(tagIds) : tagIds.stream().anyMatch(trackTagIds::contains);
    }

    // trackId -> ids de ses tags. getTrack()/getTag() ne déclenchent pas de chargement de l'association Lazy :
    // Hibernate connaît déjà l'id via la clé étrangère, donc pas besoin de charger le reste de l'entité.
    private Map<Integer, Set<Integer>> tagIdsByTrackId() {
        return trackTagRepository.findAll().stream()
            .collect(Collectors.groupingBy(
                trackTag -> trackTag.getTrack().getId(),
                Collectors.mapping(trackTag -> trackTag.getTag().getId(), Collectors.toSet())
            ));
    }

    private double knowsRatio(List<Knowledge> votes) {
        long known = votes.stream().filter(Knowledge::isKnows).count();
        return (double) known / votes.size();
    }
}
