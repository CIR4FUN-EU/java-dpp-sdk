package demo.producer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Entry point for the internal demo modes.
 *
 * <p>This class only routes command-line arguments to the selected demo runner. It is not production
 * orchestration logic.</p>
 */
@Component
class DemoRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        String[] nonOptionArgs = nonOptionArgs(args);
        String mode = modeOf(nonOptionArgs);
        String[] urlArgs = urlArgs(nonOptionArgs, mode);

        if ("sdk".equals(mode)) {
            new SdkCapabilityDemoRunner().run();
            return;
        }

        if ("all".equals(mode) || "sdk-http".equals(mode)) {
            DemoDppSamples samples = new SdkCapabilityDemoRunner().run();
            new HttpServiceDemoRunner().run(samples, urlArgs);
            return;
        }

        new HttpServiceDemoRunner().run(urlArgs);
    }

    String[] nonOptionArgs(String[] args) {
        return java.util.Arrays.stream(args)
                .filter(arg -> !arg.startsWith("--"))
                .toArray(String[]::new);
    }

    String modeOf(String[] nonOptionArgs) {
        if (nonOptionArgs.length == 0) {
            return "standards";
        }
        String first = nonOptionArgs[0].toLowerCase();
        return switch (first) {
            case "sdk", "http", "all", "sdk-http", "standards" -> first;
            default -> "standards";
        };
    }

    String[] urlArgs(String[] nonOptionArgs, String mode) {
        if (nonOptionArgs.length == 0) {
            return nonOptionArgs;
        }
        boolean explicitMode = switch (mode) {
            case "sdk", "http", "all", "sdk-http", "standards" -> mode.equalsIgnoreCase(nonOptionArgs[0]);
            default -> false;
        };
        if (!explicitMode) {
            return nonOptionArgs;
        }
        return java.util.Arrays.copyOfRange(nonOptionArgs, 1, nonOptionArgs.length);
    }
}
