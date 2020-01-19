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
     * @param fileName File name value.
     * @apiNote Supported variables:
     * - %plugin% - The name of the plugin in Bukkit.
     * - %old_version% - The old/current version of the plugin.
     * - %new_version% - The new version of the plugin that is being downloaded.
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
     * @param updatingMsg The message value.
     * @apiNote Supported variables:
     * - %status% - Detailed status message.
     * - %plugin% - The name of the plugin in Bukkit.
     * - %old_version% - The old/current version of the plugin.
     * - %new_version% - The new version of the plugin that is being downloaded.
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
     * @apiNote Supported variables (despite name):
     * - %status% - Detailed status message.
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
     * @param downloadingMsg The message value.
     * @apiNote Supported variables:
     * - %status% - Detailed status message.
     * - %plugin% - The name of the plugin in Bukkit.
     * - %old_version% - The old/current version of the plugin.
     * - %new_version% - The new version of the plugin that is being downloaded.
     * - %download_bar% - A progress bar of the download.
     * - %download_percent% - A percentage of the download.
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
     * @param completionMsg The message value.
     * @apiNote Supported variables:
     * - %status% - Detailed status message.
     * - %plugin% - The name of the plugin in Bukkit.
     * - %old_version% - The old/current version of the plugin.
     * - %new_version% - The new version of the plugin that was downloaded.
     * - %elapsed_time% - The elapsed time of the update in seconds.
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
     * @apiNote Supported variables:
     * - %status% - Detailed status message.
     */
    void setFailureMsg(String failureMsg);

    /**
     * Builder for this class.
     */
    interface LocaleBuilder {
        /**
         * Sets of the plugin AutoUpdater will try to enable after the download is complete.
         *
         * @param fileName File name value.
         * @apiNote Supported variables:
         * - %plugin% - The name of the plugin in Bukkit.
         * - %old_version% - The old/current version of the plugin.
         * - %new_version% - The new version of the plugin that is being downloaded.
         *
         * @return Builder instance.
         */
        LocaleBuilder fileName(String fileName);

        /**
         * Sets of the plugin AutoUpdater will try to enable after the download is complete.
         *
         * @param bukkitPluginName Name of the Bukkit plugin.
         *
         * @return Builder instance.
         */
        LocaleBuilder bukkitPluginName(String bukkitPluginName);

        /**
         * Sets the message sent to an update initiator while an update is ongoing.
         *
         * @param updatingMsg The message value.
         * @apiNote Supported variables:
         * - %status% - Detailed status message.
         * - %plugin% - The name of the plugin in Bukkit.
         * - %old_version% - The old/current version of the plugin.
         * - %new_version% - The new version of the plugin that is being downloaded.
         *
         * @return Builder instance.
         */
        LocaleBuilder updatingMsg(String updatingMsg);

        /**
         * Gets the updating message to be sent before variables have been updated.
         *
         * @param updatingMsgNoVar The message value.
         * @apiNote Supported variables (despite name):
         * - %status% - Detailed status message.
         *
         * @return Builder instance.
         */
        LocaleBuilder updatingMsgNoVar(String updatingMsgNoVar);

        /**
         * Sets the message sent to players while a download is occurring.
         *
         * @param downloadingMsg The message value.
         * @apiNote Supported variables:
         * - %status% - Detailed status message.
         * - %plugin% - The name of the plugin in Bukkit.
         * - %old_version% - The old/current version of the plugin.
         * - %new_version% - The new version of the plugin that is being downloaded.
         * - %download_bar% - A progress bar of the download.
         * - %download_percent% - A percentage of the download.
         *
         * @return Builder instance.
         */
        LocaleBuilder downloadingMsg(String downloadingMsg);

        /**
         * Sets the message sent to players upon update completion.
         *
         * @param completionMsg The message value.
         * @apiNote Supported variables:
         * - %status% - Detailed status message.
         * - %plugin% - The name of the plugin in Bukkit.
         * - %old_version% - The old/current version of the plugin.
         * - %new_version% - The new version of the plugin that was downloaded.
         * - %elapsed_time% - The elapsed time of the update in seconds.
         *
         * @return Builder instance.
         */
        LocaleBuilder completionMsg(String completionMsg);

        /**
         * Sets the message sent to players upon update failure.
         *
         * @param failureMsg The message value.
         * @apiNote Supported variables:
         * - %status% - Detailed status message.
         *
         * @return Builder instance.
         */
        LocaleBuilder failureMsg(String failureMsg);

        UpdateLocale build();
    }
}
