package fr.insalan.blindtest.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.insalan.blindtest.dto.TagTypeResponse;
import fr.insalan.blindtest.repository.TagTypeRepository;

@RestController
@RequestMapping("/api/tag-types")
public class TagTypeController {

    private final TagTypeRepository tagTypeRepository;

    public TagTypeController(TagTypeRepository tagTypeRepository) {
        this.tagTypeRepository = tagTypeRepository;
    }

    @GetMapping
    public List<TagTypeResponse> list() {
        return tagTypeRepository.findAll().stream()
            .map(TagTypeResponse::from)
            .toList();
    }
}
