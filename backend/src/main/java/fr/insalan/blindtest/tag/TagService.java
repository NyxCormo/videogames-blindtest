package fr.insalan.blindtest.tag;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import fr.insalan.blindtest.model.Tag;
import fr.insalan.blindtest.model.TagType;
import fr.insalan.blindtest.repository.TagRepository;
import fr.insalan.blindtest.repository.TagTypeRepository;
import fr.insalan.blindtest.repository.TrackTagRepository;
import jakarta.transaction.Transactional;

@Service
public class TagService {

    private final TagTypeRepository tagTypeRepository;
    private final TagRepository tagRepository;
    private final TrackTagRepository trackTagRepository;

    public TagService(
        TagTypeRepository tagTypeRepository,
        TagRepository tagRepository,
        TrackTagRepository trackTagRepository
    ){
        this.tagTypeRepository = tagTypeRepository;
        this.tagRepository = tagRepository;
        this.trackTagRepository = trackTagRepository;
    }

    @Transactional
    public Tag findOrCreate(String typeName, String tagName) {
        TagType type = tagTypeRepository.findByName(typeName)
            .orElseGet(() -> tagTypeRepository.save(new TagType(typeName)));
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
}
