package cc.flogi.dev.autoupdater.internal;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 9/2/19
 */
public class PropertiesTest {
    @Test public void readPropertiesTest() {
        //Expected values.
        final String author = "flogic";
        final String repoUrl = "https://github.com/fl0gic/AutoUpdaterAPI/";

        ProjectProperties properties = ProjectProperties.from("autoupdater.properties");

        Assert.assertEquals(author, properties.AUTHOR);
        Assert.assertEquals(repoUrl, properties.REPO_URL);
    }
}
