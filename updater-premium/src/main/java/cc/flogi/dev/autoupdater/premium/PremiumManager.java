package cc.flogi.dev.autoupdater.premium;

import cc.flogi.dev.autoupdater.AutoUpdaterAPI;
import cc.flogi.dev.autoupdater.UpdateLocale;
import cc.flogi.dev.autoupdater.premium.util.UtilSpigotCreds;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 11/30/19
 */
public class PremiumManager {
    @Getter @Setter private User currentUser;
    @Getter private SpigotSiteAPI api;
    @Getter private WebClient webClient;

    public PremiumManager() {
        //Setup spigot credential files.
        UtilSpigotCreds.getInstance().init();

        try {
            LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log",
                    "org.apache.commons.logging.impl.NoOpLog");
            java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
        } catch (Exception ex) {
            printError(ex, "Unable to turn off HTML unit logging!.");
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
                api = new SpigotSiteCore();

                UtilSpigotCreds.getInstance().updateKeys();

                try {
                    if (UtilSpigotCreds.getInstance().getUsername() != null && UtilSpigotCreds.getInstance().getPassword() != null) {
                        logger.info("Stored credentials detected, attempting login.");
                        new PremiumUpdater(null, plugin, 1, new UpdateLocale(), false).authenticate(false);
                    }
                } catch (Exception ex) {
                    printError(ex, "Error occurred while initializing the spigot site API.");
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void resetUser() {
        AutoUpdaterAPI.get().setCurrentUser(null);
        UtilSpigotCreds.getInstance().reset();
    }
}
