package fr.insalan.blindtest.tag;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import fr.insalan.blindtest.model.Tag;
import fr.insalan.blindtest.model.TagType;
import fr.insalan.blindtest.model.Track;
import fr.insalan.blindtest.model.TrackTag;
import fr.insalan.blindtest.model.TrackTagId;
import fr.insalan.blindtest.repository.TagRepository;
import fr.insalan.blindtest.repository.TagTypeRepository;
import fr.insalan.blindtest.repository.TrackRepository;
import fr.insalan.blindtest.repository.TrackTagRepository;
import jakarta.transaction.Transactional;

@Service
public class TagService {

    private final TagTypeRepository tagTypeRepository;
    private final TagRepository tagRepository;
    private final TrackTagRepository trackTagRepository;
    private final TrackRepository trackRepository;

    public TagService(
        TagTypeRepository tagTypeRepository,
        TagRepository tagRepository,
        TrackTagRepository trackTagRepository,
        TrackRepository trackRepository
    ){
        this.tagTypeRepository = tagTypeRepository;
        this.tagRepository = tagRepository;
        this.trackTagRepository = trackTagRepository;
        this.trackRepository = trackRepository;
    }

    @Transactional
    public Tag create(Integer typeId, String tagName) {
        TagType type = tagTypeRepository.findById(typeId)
            .orElseThrow(() -> new IllegalArgumentException("Type de tag inconnu"));
        return tagRepository.findByTypeAndName(type, tagName)
            .orElseGet(() -> tagRepository.save(new Tag(tagName, type)));
    }

    // Les tags les plus utilisés à travers toute la base, pour donner des exemples au moment d'en choisir un.
    @Transactional
    public List<TagUsage> mostUsed(int limit) {
        List<Object[]> counts = trackTagRepository.countTracksByTag(PageRequest.of(0, limit));
        List<Integer> tagIds = counts.stream().map(row -> (Integer) row[0]).toList();
        Map<Integer, Tag> tagsById = tagRepository.findAllByIdInWithType(tagIds).stream()
            .collect(Collectors.toMap(Tag::getId, tag -> tag));
        return counts.stream()
            .map(row -> new TagUsage(tagsById.get((Integer) row[0]), (Long) row[1]))
            .toList();
    }

    // Applique un tag à toutes les musiques d'un jeu (utilisé pour les tags inhérents au jeu : genre, plateforme).
    // Idempotent : une musique qui a déjà le tag n'est pas touchée deux fois.
    @Transactional
    public void applyToGame(Integer tagId, Integer gameId) {
        Tag tag = tagRepository.findById(tagId).orElseThrow();
        for (Track track : trackRepository.findByGameIdWithGameAndFranchise(gameId)) {
            TrackTagId trackTagId = new TrackTagId(track.getId(), tag.getId());
            if (!trackTagRepository.existsById(trackTagId)) {
                trackTagRepository.save(new TrackTag(track, tag));
            }
        }
    }
}
