package fr.insalan.blindtest.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import fr.insalan.blindtest.model.AlbumScan;

public interface AlbumScanRepository extends JpaRepository<AlbumScan, String> {

    @Query("SELECT DISTINCT s FROM AlbumScan s LEFT JOIN FETCH s.images")
    List<AlbumScan> findAllWithImages();

    @Query("SELECT count(s) FROM AlbumScan s WHERE s.images IS EMPTY")
    long countWithoutImage();
}
