package fr.insalan.blindtest.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import fr.insalan.blindtest.deletion.DeletePreview;
import fr.insalan.blindtest.deletion.DeletionService;
import fr.insalan.blindtest.dto.MergeRequest;
import fr.insalan.blindtest.dto.RenameRequest;
import fr.insalan.blindtest.dto.TagCountResponse;
import fr.insalan.blindtest.dto.TagResponse;
import fr.insalan.blindtest.merge.MergePreview;
import fr.insalan.blindtest.merge.MergeService;
import fr.insalan.blindtest.model.Tag;
import fr.insalan.blindtest.repository.TagRepository;

@RestController
@RequestMapping("/api/admin/tags")
public class AdminTagController {

    private final TagRepository tagRepository;
    private final MergeService mergeService;
    private final DeletionService deletionService;

    public AdminTagController(TagRepository tagRepository, MergeService mergeService, DeletionService deletionService) {
        this.tagRepository = tagRepository;
        this.mergeService = mergeService;
        this.deletionService = deletionService;
    }

    // Tous les tags avec le nombre de musiques qui les portent, pour repérer les fautes et les doublons.
    @GetMapping
    public List<TagCountResponse> list() {
        return tagRepository.findAllWithCount().stream()
            .map(TagCountResponse::from)
            .toList();
    }

    @PatchMapping("/{id}")
    public TagResponse rename(@PathVariable Integer id, @RequestBody RenameRequest request) {
        Tag tag = tagRepository.findByIdWithType(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag introuvable"));
        String name = AdminNames.require(request.name());
        if (tagRepository.existsByTypeAndNameIgnoreCaseAndIdNot(tag.getType(), name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le tag « " + name + " » existe déjà dans ce type");
        }
        tag.setName(name);
        tagRepository.save(tag);
        return TagResponse.from(tag);
    }

    @GetMapping("/{id}/merge-preview")
    public MergePreview mergePreview(@PathVariable Integer id, @RequestParam Integer into) {
        return mergeService.previewTags(id, into);
    }

    @PostMapping("/{id}/merge")
    public TagResponse merge(@PathVariable Integer id, @RequestBody MergeRequest request) {
        mergeService.mergeTags(id, request.targetId());
        return TagResponse.from(tagRepository.findByIdWithType(request.targetId()).orElseThrow());
    }

    @GetMapping("/{id}/delete-preview")
    public DeletePreview deletePreview(@PathVariable Integer id) {
        return deletionService.previewTag(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        deletionService.deleteTag(id);
    }
}
