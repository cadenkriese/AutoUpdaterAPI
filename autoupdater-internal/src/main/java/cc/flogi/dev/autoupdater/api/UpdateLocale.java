package cc.flogi.dev.autoupdater.api;

/**
 * @author Caden Kriese (flogic)
 *
 * Used for managing messages sent to users during the update process,
 * use it to add your own localization or stylistic changes.
 *
 * Note that all messages are sent through action bars.
 * Also, download progress bars are unlikely to be seen as most downloads finish extremely quickly.
 * Short status messages will be appended to the end of these messages.
 *
 * For example, while an update is occuring a player may see this in their ActionBar.
 * UPDATING PLUGIN... [RETRIEVING FILES]
 *
 * Created on 01/14/2020
 */
public interface UpdateLocale {

    /**
     * What the downloaded plugin jar in the download's folder should be named.
     *
     * @return String value.
     */
    String getFileName();

    /**
     * Sets of the plugin AutoUpdater will try to enable after the download is complete.
     *
     * @apiNote Supported variables:
     * - %plugin% - The name of the plugin in Bukkit.
     * - %old_version% - The old/current version of the plugin.
     * - %new_version% - The new version of the plugin that is being downloaded.
     *
     * @param fileName File name value.
     */
    void setFileName(String fileName);

    /**
     * Gets of the plugin AutoUpdater will try to enable after the download is complete.
     *
     * @return The plugin name value.
     */
    String getBukkitPluginName();

    /**
     * Sets of the plugin AutoUpdater will try to enable after the download is complete.
     *
     * @param bukkitPluginName Name of the Bukkit plugin.
     */
    void setBukkitPluginName(String bukkitPluginName);

    /**
     * Gets the message sent to an update initiator while an update is ongoing.
     *
     * @return The message value.
     */
    String getUpdatingMsg();

    /**
     * Sets the message sent to an update initiator while an update is ongoing.
     *
     * @apiNote Supported variables:
     * - %plugin% - The name of the plugin in Bukkit.
     * - %old_version% - The old/current version of the plugin.
     * - %new_version% - The new version of the plugin that is being downloaded.
     *
     * @param updatingMsg The message value.
     */
    void setUpdatingMsg(String updatingMsg);

    /**
     * Gets the updating message to be sent before variables have been updated.
     *
     * @return The message value.
     */
    String getUpdatingMsgNoVar();

    /**
     * Gets the updating message to be sent before variables have been updated.
     *
     * @param updatingMsgNoVar The message value.
     */
    void setUpdatingMsgNoVar(String updatingMsgNoVar);

    /**
     * Gets the message sent to players while a download is occurring.
     *
     * @return The message value.
     */
    String getDownloadingMsg();

    /**
     * Sets the message sent to players while a download is occurring.
     *
     * @apiNote Supported variables:
     * - %download_bar% - A progress bar of the download.
     * - %download_percent% - A percentage of the download.
     *
     * @param downloadingMsg The message value.
     */
    void setDownloadingMsg(String downloadingMsg);

    /**
     * Gets the message sent to players upon update completion.
     *
     * @return The message value.
     */
    String getCompletionMsg();

    /**
     * Sets the message sent to players upon update completion.
     *
     * @apiNote Supported variables:
     * - %plugin% - The name of the plugin in Bukkit.
     * - %old_version% - The old/current version of the plugin.
     * - %new_version% - The new version of the plugin that was downloaded.
     * - %elapsed_time% - The elapsed time of the update in seconds.
     *
     * @param completionMsg The message value.
     */
    void setCompletionMsg(String completionMsg);

    /**
     * Gets the message sent to players upon update failure.
     *
     * @return The message value.
     */
    String getFailureMsg();

    /**
     * Sets the message sent to players upon update failure.
     *
     * @param failureMsg The message value.
     */
    void setFailureMsg(String failureMsg);

    /**
     * Builder for this class.
     */
    interface LocaleBuilder {
        LocaleBuilder fileName(String fileName);
        LocaleBuilder bukkitPluginName(String bukkitPluginName);
        LocaleBuilder updatingMsg(String updatingMsg);
        LocaleBuilder updatingMsgNoVar(String updatingMsgNoVar);
        LocaleBuilder downloadingMsg(String downloadingMsg);
        LocaleBuilder completionMsg(String completionMsg);
        LocaleBuilder failureMsg(String failureMsg);
        UpdateLocale build();
    }
}
