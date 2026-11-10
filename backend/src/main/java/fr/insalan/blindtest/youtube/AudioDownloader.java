package fr.insalan.blindtest.youtube;

import java.io.IOException;
import java.nio.file.Path;

public interface AudioDownloader {

    Path download(String youtubeUrl, Path directory) throws IOException;
}
