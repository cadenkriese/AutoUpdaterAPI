package cc.flogi.dev.autoupdater.internal;

import cc.flogi.dev.autoupdater.api.UpdateLocale;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 6/6/17
 */
@Getter @Setter @AllArgsConstructor
public final class PluginUpdateLocale implements UpdateLocale {
    private String fileName;
    private String bukkitPluginName;
    private String updatingMsg;
    private String updatingMsgNoVar;
    private String downloadingMsg;
    private String completionMsg;
    private String failureMsg;

    public static UpdateLocaleBuilder builder() {return new UpdateLocaleBuilder();}

    /**
     * Replaces the variables in all the fields.
     *
     * @param pluginName The name of the plugin in the context of this UpdateLocale.
     * @param oldVersion The old / current version of the plugin in the context of this UpdateLocale.
     * @param newVersion The new version of the plugin in the context of this UpdateLocale.
     * @throws IllegalAccessException This should never happen but it may for some peculiar reason.
     */
    public void updateVariables(String pluginName, String oldVersion, String newVersion) throws IllegalAccessException {
        if (!fileName.endsWith(".jar"))
            fileName += ".jar";
        for (Field field : Arrays.stream(PluginUpdateLocale.class.getDeclaredFields())
                .filter(f -> f.getType().equals(String.class))
                .collect(Collectors.toList())) {

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
    }

    public static class UpdateLocaleBuilder implements LocaleBuilder {
        private String fileName = "plugin";
        private String bukkitPluginName = "plugin";
        private String updatingMsg = "&f&lUPDATING &1&l%plugin% &b&l%old_version% &a&l» &b&l%new_version% &8[%status%]";
        private String updatingMsgNoVar = "&f&lUPDATING PLUGIN... &8[%status%]";
        private String downloadingMsg = "&f&lDOWNLOADING &1&l%plugin% &b&l%new_version% &8| %download_bar% &8| &a%download_percent% &8[%status%]";
        private String completionMsg = "&f&lUPDATED &1&l%plugin% &f&lTO &b&l%new_version% &7&o(%elapsed_time%s) &8[%status%]";
        private String failureMsg = "&c&lUPDATE FAILED &8[%status%]";

        UpdateLocaleBuilder() {}

        public UpdateLocaleBuilder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public UpdateLocaleBuilder bukkitPluginName(String bukkitPluginName) {
            this.bukkitPluginName = bukkitPluginName;
            return this;
        }

        public UpdateLocaleBuilder updatingMsg(String updatingMsg) {
            this.updatingMsg = updatingMsg;
            return this;
        }

        public UpdateLocaleBuilder updatingMsgNoVar(String updatingMsgNoVar) {
            this.updatingMsgNoVar = updatingMsgNoVar;
            return this;
        }

        public UpdateLocaleBuilder downloadingMsg(String downloadingMsg) {
            this.downloadingMsg = downloadingMsg;
            return this;
        }

        public UpdateLocaleBuilder completionMsg(String completionMsg) {
            this.completionMsg = completionMsg;
            return this;
        }

        public UpdateLocaleBuilder failureMsg(String failureMsg) {
            this.failureMsg = failureMsg;
            return this;
        }

        public PluginUpdateLocale build() {
            return new PluginUpdateLocale(fileName, bukkitPluginName, updatingMsg, updatingMsgNoVar, downloadingMsg, completionMsg, failureMsg);
        }
    }
}

