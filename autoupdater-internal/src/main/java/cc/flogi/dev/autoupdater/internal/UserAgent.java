package cc.flogi.dev.autoupdater.internal;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 12/12/19
 */
enum UserAgent {
    CHROME("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.79 Safari/537.36"),
    SPIGET("AutoUpdaterAPI");

    private String agent;

    UserAgent(String agent) {
        this.agent = agent;
    }

    public String toString() {
        return agent;
    }
}
