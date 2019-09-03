package cc.flogi.dev.autoupdaterapi.util;

import java.io.IOException;
import java.util.Properties;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 9/1/19
 */
public class ProjectProperties {
    public ProjectProperties(Properties properties) {
        NAME = properties.getProperty("name");
        DESCRIPTION = properties.getProperty("description");
        URL = properties.getProperty("url");
        REPO_URL = properties.getProperty("repo_url");
        AUTHOR = properties.getProperty("author");
        VERSION = properties.getProperty("version");
    }

    public static ProjectProperties from(String resource) {
        try {
            Properties properties = new Properties();
            properties.load(ProjectProperties.class.getResourceAsStream("/"+resource));
            return new ProjectProperties(properties);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public final String NAME;
    public final String DESCRIPTION;
    public final String URL;
    public final String REPO_URL;
    public final String AUTHOR;
    public final String VERSION;

    public String getTitle() {
        return NAME + " V" + VERSION;
    }

    public String getTitleWithAuthor() {
        return NAME + " V" + VERSION + " by " + AUTHOR;
    }
}
