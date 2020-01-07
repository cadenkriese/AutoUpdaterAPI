package cc.flogi.dev.autoupdater.api;

/**
 * @author Caden Kriese
 *
 * Created on 01/07/2020.
 */
public interface SpigotPluginUpdater extends PluginUpdater {
    /**
     * Returns the plugins supported versions from Spigot.
     *
     * @return The list of supported Minecraft versions as listed on the plugin Spigot page.
     */
    String[] getSupportedVersions();

    /**
     * Checks if the current MC version is supported by this plugin.
     *
     * @return If the current version is listed as supported in the resource's Spigot page.
     */
    Boolean currentVersionSupported();

    /**
     * Retrieves the average rating of the resource on spigot.
     *
     * @return The average rating of the resource across all versions.
     */
    Double getAverageRating();
}
