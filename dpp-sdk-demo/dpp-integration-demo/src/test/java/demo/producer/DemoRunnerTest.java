package demo.producer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DemoRunnerTest {

    private final DemoRunner runner = new DemoRunner();

    @Test
    @DisplayName("Defaults to the standards HTTP demo when no explicit mode is provided")
    void noModeDefaultsToStandards() {
        String[] args = runner.nonOptionArgs(new String[] {"--debug=false"});

        assertEquals("standards", runner.modeOf(args));
        assertArrayEquals(new String[0], runner.urlArgs(args, "standards"));
    }

    @Test
    @DisplayName("HTTP mode keeps the explicit registry and repository URLs")
    void httpModeKeepsUrlArguments() {
        String[] args = runner.nonOptionArgs(new String[] {
                "http",
                "http://localhost:8081",
                "http://localhost:8080",
                "--debug=false"
        });

        assertEquals("http", runner.modeOf(args));
        assertArrayEquals(
                new String[] {"http://localhost:8081", "http://localhost:8080"},
                runner.urlArgs(args, "http")
        );
    }

    @Test
    @DisplayName("Bare URL arguments still run through the standards HTTP demo mode")
    void urlsWithoutModeStillRunHttp() {
        String[] args = runner.nonOptionArgs(new String[] {
                "http://localhost:8081",
                "http://localhost:8080",
                "--debug=false"
        });

        assertEquals("standards", runner.modeOf(args));
        assertArrayEquals(
                new String[] {"http://localhost:8081", "http://localhost:8080"},
                runner.urlArgs(args, "standards")
        );
    }

    @Test
    @DisplayName("Recognizes the supported demo mode names")
    void supportsSdkAndCombinedModes() {
        assertEquals("sdk", runner.modeOf(new String[] {"sdk"}));
        assertEquals("all", runner.modeOf(new String[] {"all"}));
        assertEquals("sdk-http", runner.modeOf(new String[] {"sdk-http"}));
        assertEquals("standards", runner.modeOf(new String[] {"standards"}));
    }

}
