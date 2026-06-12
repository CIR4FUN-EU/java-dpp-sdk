package demo.producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Path;

@SpringBootApplication
public class DppProducerApplication {

    public static void main(String[] args) {
        DotenvPropertyLoader.loadIfPresent(Path.of(".env"));
        ConfigurableApplicationContext context = SpringApplication.run(DppProducerApplication.class, args);
        System.exit(SpringApplication.exit(context));
    }
}
