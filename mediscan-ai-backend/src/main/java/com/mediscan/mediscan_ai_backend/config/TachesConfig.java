package com.mediscan.mediscan_ai_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Active :
 * - @Async      : envoi des notifications en tâche de fond (le scan n'attend pas Google) ;
 * - @Scheduled  : vérification des rappels de prise chaque minute.
 */
@Configuration
@EnableAsync
@EnableScheduling
public class TachesConfig {
}