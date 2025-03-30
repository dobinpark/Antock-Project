package antock.Antock_Project.common.config;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Configuration
public class WebDriverConfig {

    @EventListener(ContextRefreshedEvent.class)
    public void setupWebDriver() {
        // 애플리케이션 시작 시 WebDriver 초기화
        WebDriverManager.chromedriver().setup();
    }
}
