import cc.flogi.dev.autoupdaterapi.UpdateLocale;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 9/2/19
 */
public class UpdateLocaleTest {
    @Test public void allVariablesGivenTest() {
        //Expected value
        final String pluginName = "Example Plugin V1.1.0 -> V1.1.1";

        UpdateLocale locale = UpdateLocale.builder().pluginName("%plugin% V%old_version% -> V%new_version%").build();
        locale.updateVariables("Example Plugin", "1.1.0", "1.1.1");

        Assert.assertEquals(locale.getPluginName(), pluginName);
    }

    @Test public void randomNullVariableTest() {
        //Expected value
        final String pluginName = "Example Plugin V1.1.0";

        UpdateLocale locale = UpdateLocale.builder().pluginName("%plugin% V%old_version%").build();
        locale.updateVariables("Example Plugin", "1.1.0", null);

        Assert.assertEquals(locale.getPluginName(), pluginName);
    }
}
