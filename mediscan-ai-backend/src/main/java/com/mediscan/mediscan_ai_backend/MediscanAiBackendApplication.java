package com.mediscan.mediscan_ai_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée du backend MediaScan AI.
 * @SpringBootApplication active l'auto-configuration et le scan
 * de tous les composants situés dans le package com.mediscan et ses sous-packages.
 */
@SpringBootApplication
public class MediscanAiBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MediscanAiBackendApplication.class, args);
	}
}