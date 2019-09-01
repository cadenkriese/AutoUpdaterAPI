package cc.flogi.dev.autoupdaterapi;

import be.maximvdw.spigotsite.SpigotSiteCore;
import be.maximvdw.spigotsite.api.SpigotSiteAPI;
import be.maximvdw.spigotsite.api.user.User;
import cc.flogi.dev.autoupdaterapi.util.ProjectProperties;
import cc.flogi.dev.autoupdaterapi.util.UtilSpigotCreds;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.logging.LogFactory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


/*
 * AutoUpdaterAPI
 *
 * Author: Caden Kriese (flogic)
 *
 * This resource is licensed under the Apache License Version 2.0.
 * Full license information in the LICENSE file.
 *
 */

public class AutoUpdaterAPI {
    private static AutoUpdaterAPI instance;
    public static AutoUpdaterAPI get() {
        return instance;
    }

    @Getter @Setter private User currentUser;
    @Getter private static Plugin plugin;
    @Getter private SpigotSiteAPI api;
    @Getter private WebClient webClient;
    @Getter private Logger logger;
    @Getter private File dataFolder;

    public final static ProjectProperties PROPERTIES = ProjectProperties.from("project.properties");
    public static final boolean DEBUG = false;

    public void init(Plugin javaPlugin) {
        //General setup
        plugin = javaPlugin;
        instance = this;
        logger = LogManager.getLogManager().getLogger(getClass().getName());
        dataFolder = new File(plugin.getDataFolder().getParent()+"/.auapi/");

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

    public void printError(Exception ex) {
        this.logger.severe("A severe error has occurred.");
        this.logger.severe("If you cannot figure out this error on your own (e.g. a config error) please copy and paste everything from here to END ERROR and post it at "+PROPERTIES.repoUrl+"issues.");
        this.logger.severe("");
        this.logger.severe("============== BEGIN ERROR ==============");
        this.logger.severe("PLUGIN VERSION: "+PROPERTIES.getTitle());
        this.logger.severe("");
        this.logger.severe("MESSAGE: " + ex.getMessage());
        this.logger.severe("");
        this.logger.severe("STACKTRACE: ");
        ex.printStackTrace();
        this.logger.severe("");
        this.logger.severe("============== END ERROR ==============");
    }

    public void printError(Exception ex, String extraInfo) {
        this.logger.severe("A severe error has occurred.");
        this.logger.severe("If you cannot figure out this error on your own (e.g. a config error) please copy and paste everything from here to END ERROR and post it at "+PROPERTIES.repoUrl+"issues.");
        this.logger.severe("");
        this.logger.severe("============== BEGIN ERROR ==============");
        this.logger.severe("PLUGIN VERSION: "+PROPERTIES.getTitle());
        this.logger.severe("");
        this.logger.severe("PLUGIN MESSAGE: " + extraInfo);
        this.logger.severe("");
        this.logger.severe("MESSAGE: " + ex.getMessage());
        this.logger.severe("");
        this.logger.severe("STACKTRACE: ");
        ex.printStackTrace();
        this.logger.severe("");
        this.logger.severe("============== END ERROR ==============");
    }

    public void printPluginError(String header, String message) {
        this.logger.severe("============== BEGIN ERROR ==============");
        this.logger.severe(header);
        this.logger.severe("");
        this.logger.severe("PLUGIN VERSION: "+PROPERTIES.getTitle());
        this.logger.severe("");
        this.logger.severe("PLUGIN MESSAGE: " + message);
        this.logger.severe("");
        this.logger.severe("============== END ERROR ==============");
    }
}
