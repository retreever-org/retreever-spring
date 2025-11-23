package project.retreever.boot;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import project.retreever.engine.ControllerScanner;
import project.retreever.engine.RetreeverOrchestrator;
import project.retreever.view.dto.ApiDocument;

import java.time.Instant;
import java.util.Set;
import java.util.logging.Logger;

@Component
public class RetreeverBootstrap {

    Logger log = Logger.getLogger(RetreeverBootstrap.class.getName());

    private final RetreeverOrchestrator orchestrator;
    private ApiDocument cached;

    public RetreeverBootstrap(RetreeverOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init(ApplicationReadyEvent event) {
        log.info("Initializing Retreever API Document...");
        ApplicationContext context = event.getApplicationContext();

        Class<?> appClass = event.getSpringApplication()
                .getMainApplicationClass();

        Set<Class<?>> controllers = ControllerScanner.scanControllers(context);

        this.cached = orchestrator.build(appClass, controllers);

        log.info("✅ Retreever API Document built successfully.");
    }

    public ApiDocument getDocument() {
        return cached;
    }

    public Instant getUptime() {
        return cached.upTime();
    }
}
