package cc.flogi.dev.autoupdater;

import be.maximvdw.spigotsite.SpigotSiteCore;
import be.maximvdw.spigotsite.api.SpigotSiteAPI;
import be.maximvdw.spigotsite.api.user.User;
import cc.flogi.dev.autoupdater.util.UtilSpigotCreds;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.logging.LogFactory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Caden Kriese (flogic)
 *
 * Controls any updates surrounding a premium plugin.
 *
 * Created on 11/30/19
 */
public class PremiumController {
    private static PremiumController instance;
    @Getter @Setter private User currentUser;
    @Getter private SpigotSiteAPI siteAPI;
    @Getter private Plugin plugin;
    @Getter private AutoUpdaterAPI updaterAPI;
    @Getter private WebClient webClient;
    @Getter private File privateDataFolder;

    /**
     * Instantiates a PremiumManager.
     *
     * @param javaPlugin the plugin running the API, not necessarily the one being updated.
     */
    public PremiumController(JavaPlugin javaPlugin) {
        //General setup
        instance = this;
        plugin = javaPlugin;
        updaterAPI = new AutoUpdaterAPI(javaPlugin);
        privateDataFolder = new File(javaPlugin.getDataFolder().getParent() + "/.auapi/");
        Logger logger = updaterAPI.getLogger();

        //Setup spigot credential files.
        UtilSpigotCreds.getInstance().init();

        try {
            LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log",
                    "org.apache.commons.logging.impl.NoOpLog");
            java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
        } catch (Exception ex) {
            AutoUpdaterAPI.get().printError(ex, "Unable to turn off HTML unit logging!.");
        }

        //Setup web client
        webClient = new WebClient(BrowserVersion.CHROME);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setTimeout(15000);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setRedirectEnabled(true);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setPrintContentOnFailingStatusCode(false);
        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);

        logger.info("Initializing connection with spigot...");

        //Spigot Site API
        new BukkitRunnable() {
            @Override
            public void run() {
                siteAPI = new SpigotSiteCore();

                UtilSpigotCreds.getInstance().updateKeys();

                try {
                    if (UtilSpigotCreds.getInstance().getUsername() != null && UtilSpigotCreds.getInstance().getPassword() != null) {
                        logger.info("Stored credentials detected, attempting login.");
                        new PremiumUpdater(null, javaPlugin, 1, new UpdateLocale(), false).authenticate(false);
                    }
                } catch (Exception ex) {
                    AutoUpdaterAPI.get().printError(ex, "Error occurred while initializing the spigot site API.");
                }
            }
        }.runTaskAsynchronously(javaPlugin);
    }

    public static PremiumController get() {
        return instance;
    }

    /**
     * Instantiate PremiumUpdater
     *
     * @param initiator  The player that started this action (if there is none set to null).
     * @param plugin     The instance of the outdated plugin.
     * @param resourceId The ID of the plugin on Spigot found in the url after the name.
     * @param locale     The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param deleteOld  Should the old version of the plugin be deleted and disabled.
     *
     * @return An instantiated PremiumUpdater.
     */
    protected PremiumUpdater createPremiumUpdater(Player initiator, Plugin plugin, int resourceId, UpdateLocale locale, boolean deleteOld) {
        return new PremiumUpdater(initiator, plugin, resourceId, locale, deleteOld);
    }

    /**
     * Instantiate PremiumUpdater
     *
     * @param initiator  The player that started this action (if there is none set to null).
     * @param plugin     The instance of the outdated plugin.
     * @param resourceId The ID of the plugin on Spigot found in the url after the name.
     * @param locale     The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param deleteOld  Should the old version of the plugin be deleted and disabled.
     * @param endTask    Runnable that will run once the update has completed.
     *
     * @return An instantiated PremiumUpdater.
     */
    protected PremiumUpdater createPremiumUpdater(Player initiator, Plugin plugin, int resourceId, UpdateLocale locale, boolean deleteOld, UpdaterRunnable endTask) {
        return new PremiumUpdater(initiator, plugin, resourceId, locale, deleteOld, endTask);
    }

    /**
     * Instantiate PremiumUpdater
     *
     * @param initiator  The player that started this action (if there is none set to null).
     * @param resourceId The ID of the plugin on Spigot found in the url after the name.
     * @param locale     The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param deleteOld  Should the old version of the plugin be deleted and disabled.
     *
     * @return An instantiated PremiumUpdater.
     */
    protected PremiumUpdater createPremiumSelfUpdater(Player initiator, int resourceId, UpdateLocale locale, boolean deleteOld) {
        return new PremiumUpdater(initiator, plugin, resourceId, locale, deleteOld);
    }

    /**
     * Instantiate PremiumUpdater
     *
     * @param initiator  The player that started this action (if there is none set to null).
     * @param resourceId The ID of the plugin on Spigot found in the url after the name.
     * @param locale     The locale file you want containing custom messages. Note most messages will be followed with a progress indicator like [DOWNLOADING].
     * @param deleteOld  Should the old version of the plugin be deleted and disabled.
     * @param endTask    Runnable that will run once the update has completed.
     *
     * @return An instantiated PremiumUpdater.
     */
    protected PremiumUpdater createPremiumSelfUpdater(Player initiator, int resourceId, UpdateLocale locale, boolean deleteOld, UpdaterRunnable endTask) {
        return new PremiumUpdater(initiator, plugin, resourceId, locale, deleteOld, endTask);
    }

    public void resetUser() {
        currentUser = null;
        UtilSpigotCreds.getInstance().reset();
    }
}