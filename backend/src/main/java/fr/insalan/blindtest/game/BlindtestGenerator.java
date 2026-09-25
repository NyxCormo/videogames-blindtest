package fr.insalan.blindtest.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import fr.insalan.blindtest.model.Blindtest;
import fr.insalan.blindtest.model.BlindtestDifficultyBand;
import fr.insalan.blindtest.model.BlindtestTrack;
import fr.insalan.blindtest.model.Knowledge;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.repository.BlindtestDifficultyBandRepository;
import fr.insalan.blindtest.repository.BlindtestRepository;
import fr.insalan.blindtest.repository.BlindtestTrackRepository;
import fr.insalan.blindtest.repository.KnowledgeRepository;
import fr.insalan.blindtest.repository.TrackTagRepository;
import jakarta.transaction.Transactional;

// Génère un blindtest en piochant des musiques jouables réparties sur un ou plusieurs paliers de difficulté.
@Service
public class BlindtestGenerator {

    private final BlindtestRepository blindtestRepository;
    private final BlindtestTrackRepository blindtestTrackRepository;
    private final BlindtestDifficultyBandRepository blindtestDifficultyBandRepository;
    private final KnowledgeRepository knowledgeRepository;
    private final TrackTagRepository trackTagRepository;

    public BlindtestGenerator(
        BlindtestRepository blindtestRepository,
        BlindtestTrackRepository blindtestTrackRepository,
        BlindtestDifficultyBandRepository blindtestDifficultyBandRepository,
        KnowledgeRepository knowledgeRepository,
        TrackTagRepository trackTagRepository
    ) {
        this.blindtestRepository = blindtestRepository;
        this.blindtestTrackRepository = blindtestTrackRepository;
        this.blindtestDifficultyBandRepository = blindtestDifficultyBandRepository;
        this.knowledgeRepository = knowledgeRepository;
        this.trackTagRepository = trackTagRepository;
    }

    @Transactional
    public Blindtest generate(
        String name,
        int trackCount,
        List<DifficultyBand> bands,
        List<Integer> tagIds,
        boolean matchAllTags,
        Integer maxPerGame,
        Integer maxPerFranchise,
        GenerationStrategy strategy
    ) {
        List<TrackWithRatio> candidates = eligibleTracks(tagIds == null ? List.of() : tagIds, matchAllTags);
        List<Integer> quotas = bandQuotas(bands, trackCount);

        List<GenerationStrategy> attempts = strategy != null
            ? List.of(strategy)
            : List.of(GenerationStrategy.RANDOM, GenerationStrategy.RARE_GAMES, GenerationStrategy.RARE_FRANCHISES);

        List<Track> picked = null;
        for (GenerationStrategy attempt : attempts) {
            picked = trySelect(candidates, bands, quotas, maxPerGame, maxPerFranchise, attempt);
            if (picked != null) {
                break;
            }
        }
        if (picked == null) {
            throw new IllegalStateException(
                "Pas assez de musiques disponibles pour ces critères (paliers, plafonds ou tags trop stricts)"
            );
        }

        Collections.shuffle(picked);

        Blindtest blindtest = blindtestRepository.save(new Blindtest(name, weightedAverageDifficulty(bands)));
        for (int position = 0; position < picked.size(); position++) {
            blindtestTrackRepository.save(new BlindtestTrack(blindtest, picked.get(position), position));
        }
        for (int position = 0; position < bands.size(); position++) {
            DifficultyBand band = bands.get(position);
            blindtestDifficultyBandRepository.save(
                new BlindtestDifficultyBand(blindtest, position, band.minDifficulty(), band.maxDifficulty(), band.proportion())
            );
        }
        return blindtest;
    }

    // Une tentative avec une stratégie donnée. Renvoie null (plutôt que lever une exception) si elle échoue,
    // pour que l'appelant puisse essayer la stratégie suivante.
    private List<Track> trySelect(
        List<TrackWithRatio> candidates,
        List<DifficultyBand> bands,
        List<Integer> quotas,
        Integer maxPerGame,
        Integer maxPerFranchise,
        GenerationStrategy strategy
    ) {
        Map<Integer, Long> gameFrequency = strategy == GenerationStrategy.RARE_GAMES ? frequencyByGame(candidates) : Map.of();
        Map<Integer, Long> franchiseFrequency = strategy == GenerationStrategy.RARE_FRANCHISES ? frequencyByFranchise(candidates) : Map.of();

        Set<Integer> pickedTrackIds = new HashSet<>();
        Map<Integer, Integer> gameCounts = new HashMap<>();
        Map<Integer, Integer> franchiseCounts = new HashMap<>();
        List<Track> picked = new ArrayList<>();

        for (int i = 0; i < bands.size(); i++) {
            DifficultyBand band = bands.get(i);
            int quota = quotas.get(i);
            double ratioMin = (100 - band.maxDifficulty()) / 100.0;
            double ratioMax = (100 - band.minDifficulty()) / 100.0;

            List<TrackWithRatio> eligible = candidates.stream()
                .filter(c -> !pickedTrackIds.contains(c.track().getId()))
                .filter(c -> c.ratio() >= ratioMin && c.ratio() <= ratioMax)
                .toList();

            int takenForBand = 0;
            for (TrackWithRatio candidate : orderByStrategy(eligible, strategy, gameFrequency, franchiseFrequency)) {
                if (takenForBand >= quota) {
                    break;
                }
                Track track = candidate.track();
                Integer gameId = track.getGame().getId();
                Integer franchiseId = track.getGame().getFranchise().getId();
                if (maxPerGame != null && gameCounts.getOrDefault(gameId, 0) >= maxPerGame) {
                    continue;
                }
                if (maxPerFranchise != null && franchiseCounts.getOrDefault(franchiseId, 0) >= maxPerFranchise) {
                    continue;
                }

                picked.add(track);
                pickedTrackIds.add(track.getId());
                gameCounts.merge(gameId, 1, Integer::sum);
                franchiseCounts.merge(franchiseId, 1, Integer::sum);
                takenForBand++;
            }

            if (takenForBand < quota) {
                return null;
            }
        }

        return picked;
    }

    // Mélange toujours les candidates (le hasard départage aussi les fréquences égales), puis trie si la
    // stratégie le demande : Stream.sorted est stable, l'ordre mélangé est donc conservé entre ex-æquo.
    private List<TrackWithRatio> orderByStrategy(
        List<TrackWithRatio> eligible,
        GenerationStrategy strategy,
        Map<Integer, Long> gameFrequency,
        Map<Integer, Long> franchiseFrequency
    ) {
        List<TrackWithRatio> shuffled = new ArrayList<>(eligible);
        Collections.shuffle(shuffled);

        return switch (strategy) {
            case RANDOM -> shuffled;
            case RARE_GAMES -> shuffled.stream()
                .sorted(Comparator.comparingLong(c -> gameFrequency.getOrDefault(c.track().getGame().getId(), 0L)))
                .toList();
            case RARE_FRANCHISES -> shuffled.stream()
                .sorted(Comparator.comparingLong(c -> franchiseFrequency.getOrDefault(c.track().getGame().getFranchise().getId(), 0L)))
                .toList();
        };
    }

    // Combien de musiques picher dans chaque palier : trackCount * proportion / 100, arrondi par la méthode
    // du plus grand reste pour que la somme des quotas tombe exactement sur trackCount.
    private List<Integer> bandQuotas(List<DifficultyBand> bands, int trackCount) {
        double[] exact = bands.stream().mapToDouble(band -> trackCount * band.proportion() / 100.0).toArray();
        int[] quotas = new int[bands.size()];
        int allocated = 0;
        for (int i = 0; i < bands.size(); i++) {
            quotas[i] = (int) Math.floor(exact[i]);
            allocated += quotas[i];
        }

        int remaining = trackCount - allocated;
        List<Integer> byRemainderDesc = IntStream.range(0, bands.size())
            .boxed()
            .sorted((a, b) -> Double.compare((exact[b] - quotas[b]), (exact[a] - quotas[a])))
            .toList();
        for (int i = 0; i < remaining; i++) {
            quotas[byRemainderDesc.get(i)]++;
        }

        return Arrays.stream(quotas).boxed().toList();
    }

    private int weightedAverageDifficulty(List<DifficultyBand> bands) {
        double weighted = bands.stream()
            .mapToDouble(band -> ((band.minDifficulty() + band.maxDifficulty()) / 2.0) * band.proportion())
            .sum();
        return (int) Math.round(weighted / 100.0);
    }

    // Musiques jouables ayant au moins un vote, avec leur ratio de connaissance, filtrées par tags.
    private List<TrackWithRatio> eligibleTracks(List<Integer> tagIds, boolean matchAllTags) {
        Map<Track, List<Knowledge>> votesByTrack = knowledgeRepository.findAll().stream()
            .filter(knowledge -> knowledge.getTrack().getAudioLink() != null)
            .collect(Collectors.groupingBy(Knowledge::getTrack));

        Map<Integer, Set<Integer>> tagIdsByTrackId = tagIds.isEmpty() ? Map.of() : tagIdsByTrackId();

        return votesByTrack.entrySet().stream()
            .filter(entry -> tagIds.isEmpty() || matchesTagFilter(tagIdsByTrackId.getOrDefault(entry.getKey().getId(), Set.of()), tagIds, matchAllTags))
            .map(entry -> new TrackWithRatio(entry.getKey(), knowsRatio(entry.getValue())))
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

    private Map<Integer, Long> frequencyByGame(List<TrackWithRatio> candidates) {
        return candidates.stream()
            .collect(Collectors.groupingBy(c -> c.track().getGame().getId(), Collectors.counting()));
    }

    private Map<Integer, Long> frequencyByFranchise(List<TrackWithRatio> candidates) {
        return candidates.stream()
            .collect(Collectors.groupingBy(c -> c.track().getGame().getFranchise().getId(), Collectors.counting()));
    }

    private record TrackWithRatio(Track track, double ratio) {
    }
}
