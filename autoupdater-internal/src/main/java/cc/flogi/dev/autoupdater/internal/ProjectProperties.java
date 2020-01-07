package cc.flogi.dev.autoupdater.internal;

import java.io.IOException;
import java.util.Properties;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 9/1/19
 */
final class ProjectProperties {
    final String NAME;
    final String DESCRIPTION;
    final String URL;
    final String REPO_URL;
    final String ARTIFACTORY_URL;
    final String AUTHOR;
    final String VERSION;

    protected ProjectProperties(Properties properties) {
        NAME = properties.getProperty("name");
        DESCRIPTION = properties.getProperty("description");
        URL = properties.getProperty("url");
        REPO_URL = properties.getProperty("repo_url");
        ARTIFACTORY_URL = properties.getProperty("artifactory_url");
        AUTHOR = properties.getProperty("author");
        VERSION = properties.getProperty("version");
    }

    protected static ProjectProperties from(String resource) {
        try {
            Properties properties = new Properties();
            properties.load(ProjectProperties.class.getResourceAsStream("/" + resource));
            return new ProjectProperties(properties);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    protected String getTitle() {
        return NAME + " V" + VERSION;
    }

    protected String getTitleWithAuthor() {
        return NAME + " V" + VERSION + " by " + AUTHOR;
    }
}
