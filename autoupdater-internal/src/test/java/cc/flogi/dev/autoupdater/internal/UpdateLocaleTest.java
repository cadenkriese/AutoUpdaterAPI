package cc.flogi.dev.autoupdater.internal;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 9/2/19
 */
public class UpdateLocaleTest {
    @Test public void allVariablesGivenTest() throws IllegalAccessException {
        //Expected value
        final String pluginName = "Example Plugin V1.1.0 -> V1.1.1";

        PluginUpdateLocale locale = PluginUpdateLocale.builder().bukkitPluginName("%plugin% V%old_version% -> V%new_version%").build();
        locale.updateVariables("Example Plugin", "1.1.0", "1.1.1");

        Assert.assertEquals(locale.getBukkitPluginName(), pluginName);
    }

    @Test public void randomNullVariableTest() throws IllegalAccessException {
        //Expected value
        final String pluginName = "Example Plugin V1.1.0";

        PluginUpdateLocale locale = PluginUpdateLocale.builder().bukkitPluginName("%plugin% V%old_version%").build();
        locale.updateVariables("Example Plugin", "1.1.0", null);

        Assert.assertEquals(locale.getBukkitPluginName(), pluginName);
    }
}
