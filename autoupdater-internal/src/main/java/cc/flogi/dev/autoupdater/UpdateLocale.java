package cc.flogi.dev.autoupdater;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.lang.reflect.Field;

/**
 * @author Caden Kriese (flogic)
 *
 * Used for managing messages sent to users during the update process,
 * use it to add your own localization or stylistic changes.
 *
 * Note that all messages are sent through action bars.
 * Also, download progress bars are unlikely to most downloads occurring extremely quickly.
 *
 * Created on 6/6/17
 */
@Data @Builder @AllArgsConstructor
public final class UpdateLocale {
    //Name of the jar file that should be created (.jar will be added at the end)
    private String fileName = "plugin";
    //Name of the plugin AutoUpdater will try to enable after the download is complete
    private String pluginName = "plugin";
    private String updating = "&f&lUPDATING &1&l%plugin% &b&l%old_version% &a&l» &b&l%new_version%";
    private String updatingNoVar = "&f&lUPDATING PLUGIN...";
    private String updatingDownload = "&f&lDOWNLOADING &1&l%plugin% &b&l%new_version% &8| %download_bar% &8| &a%download_percent%";
    private String updateComplete = "&f&lUPDATED &1&l%plugin% &f&lTO &b&l%new_version% &7&o(%elapsed_time%s)";
    private String updateFailed = "&c&lUPDATE FAILED";

    public UpdateLocale() {}

    /**
     * Replaces the variables in all the fields.
     *
     * @param pluginName The name of the plugin in the context of this UpdateLocale.
     * @param oldVersion The old / current version of the plugin in the context of this UpdateLocale.
     * @param newVersion The new version of the plugin in the context of this UpdateLocale.
     */
    public void updateVariables(String pluginName, String oldVersion, String newVersion) {
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
            AutoUpdaterInternal.get().printError(ex);
        }
    }
}
