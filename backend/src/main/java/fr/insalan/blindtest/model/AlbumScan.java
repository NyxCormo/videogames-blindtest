package fr.insalan.blindtest.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;

@Entity
public class AlbumScan {

    @Id
    private String albumUrl;

    @Column(nullable = false, columnDefinition = "datetime")
    private Instant scannedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "album_scan_image", joinColumns = @JoinColumn(name = "album_url"))
    @OrderColumn(name = "position")
    @Column(name = "image_url", nullable = false)
    private List<String> images = new ArrayList<>();

    protected AlbumScan() {
        // JPA
    }

    public AlbumScan(String albumUrl) {
        this.albumUrl = albumUrl;
    }

    public void record(List<String> found) {
        images.clear();
        images.addAll(found);
        scannedAt = Instant.now();
    }

    public String getAlbumUrl() {
        return albumUrl;
    }

    public Instant getScannedAt() {
        return scannedAt;
    }

    public List<String> getImages() {
        return images;
    }
}
