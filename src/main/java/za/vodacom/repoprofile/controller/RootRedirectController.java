package za.vodacom.repoprofile.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootRedirectController {

    private static final Logger log = LoggerFactory.getLogger(RootRedirectController.class);

    private final Environment environment;

    public RootRedirectController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/")
    public String redirectToSwagger() {
        return "redirect:/swagger-ui.html";
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openSwaggerOnStartup() {
        String port = environment.getProperty("server.port", "8080");
        String url = "http://localhost:" + port + "/swagger-ui.html";
        log.info("Opening Swagger UI at {}", url);
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", url});
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", url});
            } else {
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
            }
        } catch (Exception e) {
            log.warn("Could not open browser automatically: {}", e.getMessage());
        }
    }
}
