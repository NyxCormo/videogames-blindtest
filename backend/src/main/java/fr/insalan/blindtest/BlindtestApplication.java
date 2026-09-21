package fr.insalan.blindtest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BlindtestApplication {

	public static void main(String[] args) throws IOException {
		// SQLite créé automatiquement le ficiher .db, mais pas le dossier data/
		Files.createDirectories(Path.of("data"));
		SpringApplication.run(BlindtestApplication.class, args);
	}

}
