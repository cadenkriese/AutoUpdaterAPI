package cc.flogi.dev.autoupdater;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.lang.reflect.Field;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 6/6/17
 */
@Data @Builder @AllArgsConstructor
public class UpdateLocale {
    public UpdateLocale() {}

    //Name of the jar file that should be created (.jar will be added at the end)
    private String fileName = "plugin";
    //Name of the plugin AutoUpdater will try to enable after the download is complete
    private String pluginName = "plugin";
    private String updating = "&f&lUPDATING &1&l%plugin% &b&lV%old_version% &a&l» &b&l%new_version%";
    private String updatingNoVar = "&f&lUPDATING PLUGIN...";
    private String updatingDownload = "&f&lUPDATING &1&l%plugin% &b&lV%old_version% &a&l» &b&lV%new_version% &8| %download_bar% &8| &a%download_percent%";
    private String updateComplete = "&f&lUPDATED &1&l%plugin% &f&lTO &b&lV%new_version% &7&o(%elapsed_time%s)";
    private String updateFailed = "&f&lUPDATING &1&l%plugin% &b&lV%old_version% &a&l» &b&l%new_version% &8[&c&lUPDATE FAILED &7&o(Check Console)]";
    private String updateFailedNoVar = "&c&lUPDATE FAILED &7(Check Console)";

    /**
     * Replaces the variables in all the fields.
     *
     * @param pluginName The name of the plugin in the context of this UpdateLocale.
     * @param oldVersion The old / current version of the plugin in the context of this UpdateLocale.
     * @param newVersion The new version of the plugin in the context of this UpdateLocale.
     */
    public void updateVariables(String pluginName, String oldVersion, String newVersion) {
        System.out.println("UPDATING STATIC VARIABLES");

        try {
            for (Field field : UpdateLocale.class.getDeclaredFields()) {
                String value = (String) field.get(this);

                if (value != null) {
                    if (pluginName != null)
                        value = value.replace("%plugin%", pluginName);
                    if (oldVersion != null)
                        value = value.replace("%old_version%", oldVersion);
                    if (newVersion != null)
                        value = value.replace("%new_version%", newVersion);

                    field.set(this, value);
                }
            }
        } catch (IllegalAccessException ex) {
            AutoUpdaterAPI.get().printError(ex);
        }
    }
}
