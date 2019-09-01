package cc.flogi.dev.autoupdaterapi.util;

import cc.flogi.dev.autoupdaterapi.AutoUpdaterAPI;

import java.io.IOException;
import java.util.Properties;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 9/1/19
 */
public class ProjectProperties {
    public ProjectProperties(Properties properties) {
        name = properties.getProperty("name");
        description = properties.getProperty("description");
        url = properties.getProperty("url");
        repoUrl = properties.getProperty("repo_url");
        author = properties.getProperty("author");
        version = properties.getProperty("version");
    }

    public static ProjectProperties from(String resource) {
        try {
            Properties properties = new Properties();
            properties.load(AutoUpdaterAPI.class.getResourceAsStream(resource));
            return new ProjectProperties(properties);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String name;
    public String description;
    public String url;
    public String repoUrl;
    public String author;
    public String version;

    public String getTitle() {
        return name + " V" + version;
    }

    public String getTitleWithAuthor() {
        return name + " V" + version + " by " + author;
    }
}
