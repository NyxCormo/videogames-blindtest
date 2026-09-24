package fr.insalan.blindtest.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.dto.CreateTagRequest;
import fr.insalan.blindtest.dto.TagResponse;
import fr.insalan.blindtest.dto.TagUsageResponse;
import fr.insalan.blindtest.repository.TagRepository;
import fr.insalan.blindtest.tag.TagService;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagRepository tagRepository;
    private final TagService tagService;

    public TagController(TagRepository tagRepository, TagService tagService) {
        this.tagRepository = tagRepository;
        this.tagService = tagService;
    }

    @GetMapping
    public List<TagResponse> search(@RequestParam(defaultValue = "") String search) {
        return tagRepository.searchWithType(search, PageRequest.of(0, 10)).stream()
            .map(TagResponse::from)
            .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TagResponse create(@RequestBody CreateTagRequest request) {
        if (request.typeName() == null || request.typeName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le type est obligatoire");
        }
        if (request.tagName() == null || request.tagName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom du tag est obligatoire");
        }
        return TagResponse.from(tagService.findOrCreate(request.typeName(), request.tagName()));
    }

    // Exemples : les tags les plus utilisés dans toute la base.
    @GetMapping("/most-used")
    public List<TagUsageResponse> mostUsed() {
        return tagService.mostUsed(10).stream()
            .map(TagUsageResponse::from)
            .toList();
    }

    // Tous les tags, pour la liste complète filtrable côté front.
    @GetMapping("/all")
    public List<TagResponse> all() {
        return tagRepository.findAllWithType().stream()
            .map(TagResponse::from)
            .toList();
    }
}
