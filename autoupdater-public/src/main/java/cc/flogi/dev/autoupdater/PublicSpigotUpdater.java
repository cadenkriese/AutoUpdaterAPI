package cc.flogi.dev.autoupdater;

import lombok.SneakyThrows;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * @author Caden Kriese (flogic)
 *
 * Performs updates for public Spigot plugins.
 *
 * Created on 12/12/19
 */
public class PublicSpigotUpdater extends PublicUpdater {
    public static final String BASE_URL = "https://api.spiget.org/v2/resources/";

    private final String url;
    private final String resourceId;

    protected PublicSpigotUpdater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean replace) {
        super(plugin, initiator, BASE_URL + resourceId + "/download", locale, replace);
        url = BASE_URL + resourceId;
        this.resourceId = String.valueOf(resourceId);

        setNewVersion(getLatestVersion());
    }

    @SneakyThrows protected PublicSpigotUpdater(Plugin plugin, Player initiator, int resourceId, UpdateLocale locale, boolean replace, UpdaterRunnable endTask) {
        super(plugin, initiator, BASE_URL + resourceId + "/download", locale, replace);
        url = BASE_URL + resourceId;
        this.resourceId = String.valueOf(resourceId);

        setNewVersion(getLatestVersion());
    }


    /**
     * Pings spigot to retrieve the latest version of a plugin.
     *
     * @return The latest version of the plugin as a string.
     * @apiNote Makes a web request, will halt current thread shortly.
     */
    public String getLatestVersion() {
        try {
            return UtilReader.readFrom("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
        } catch (Exception exception) {
            InternalCore.get().printError(exception);
            UtilUI.sendActionBar(getInitiator(), getLocale().getUpdateFailedNoVar());
        }

        return "";
    }
}
