package fr.insalan.blindtest.tag;

import org.springframework.stereotype.Service;

import fr.insalan.blindtest.model.Tag;
import fr.insalan.blindtest.model.TagType;
import fr.insalan.blindtest.repository.TagRepository;
import fr.insalan.blindtest.repository.TagTypeRepository;
import jakarta.transaction.Transactional;

@Service 
public class TagService {
    
    private final TagTypeRepository tagTypeRepository;
    private final TagRepository tagRepository;

    public TagService(
        TagTypeRepository tagTypeRepository, 
        TagRepository tagRepository
    ){
        this.tagTypeRepository = tagTypeRepository;
        this.tagRepository = tagRepository;
    }

    @Transactional
    public Tag findOrCreate(String typeName, String tagName) {
        TagType type = tagTypeRepository.findByName(typeName)
            .orElseGet(() -> tagTypeRepository.save(new TagType(typeName)));
        return tagRepository.findByTypeAndName(type, tagName)
            .orElseGet(() -> tagRepository.save(new Tag(tagName, type)));
    }
}
