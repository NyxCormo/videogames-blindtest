package fr.insalan.blindtest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
public class BlindtestApplication {

	public static void main(String[] args) throws IOException {
		// SQLite créé automatiquement le ficiher .db, mais pas le dossier data/
		Files.createDirectories(Path.of("data"));
		SpringApplication application = new SpringApplication(BlindtestApplication.class);
		// Pas de serveur web, ni de tâches planifiées, pour une commande ponctuelle (import, rafraîchissement...)
		DefaultApplicationArguments arguments = new DefaultApplicationArguments(args);
		if (arguments.containsOption("import") || arguments.containsOption("refresh-links")) {
			application.setWebApplicationType(WebApplicationType.NONE);
			application.setAdditionalProfiles("cli");
		}
		application.run(args);
	}

	// Désactivée en mode ligne de commande (via le profile "cli") afin d'éviter que le planificateur garde le process ouvert 
	// et que la commande ne s'arrête plus seule
	@Configuration 
	@Profile("!cli")
	@EnableScheduling 
	static class SchedulingConfig {
		
	}

}
