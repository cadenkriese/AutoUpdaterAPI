package cc.flogi.dev.autoupdaterapi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

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
}
