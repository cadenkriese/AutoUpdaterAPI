package com.gamerking195.dev.autoupdaterapi;

import java.util.logging.Level;
import java.util.logging.Logger;

import be.maximvdw.spigotsite.SpigotSiteCore;
import be.maximvdw.spigotsite.api.SpigotSiteAPI;
import be.maximvdw.spigotsite.api.user.User;
import be.maximvdw.spigotsite.user.SpigotUser;
import com.gamerking195.dev.autoupdaterapi.util.UtilDownloader;
import com.gamerking195.dev.autoupdaterapi.util.UtilSpigotCreds;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.logging.LogFactory;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main
        extends JavaPlugin
{
    private static Main instance;
    private Logger log;
    public static Main getInstance()
    {
        return instance;
    }

    @Getter
    private SpigotSiteAPI api;

    @Getter
    @Setter
    private WebClient webClient;

    @Getter
    @Setter
    private User currentUser;

    @Getter
    private boolean debug = true;

    public void onEnable()
    {
        /*
         * General setup.
         */

        instance = this;
        log = getLogger();

        /*
         * HTML Unit (Using the OSGi classifier as to be compatible with MaximVdW's spigot site api)
         */

        UtilDownloader.downloadLib(UtilDownloader.Library.HTMMLUNIT);

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

        log.info("Initializing connection with spigot, this may take a while...");

        /*
         * Spigot Site API
         */

        try {
            api = new SpigotSiteCore();
        } catch (Exception ex) {
            printError(ex, "Error occurred while initializing the spigot site API.");
            return;
        }

        /*
         * Setup local files
         */

        UtilSpigotCreds.getInstance().init();

        log.info("AutoUpdaterAPI V"+getDescription().getVersion()+" enabled.!");
    }



    public void printError(Exception ex)
    {
        this.log.severe("A severe error has occurred with AutoUpdaterAPI.");
        this.log.severe("If you cannot figure out this error on your own (e.g. a config error) please copy and paste everything from here to END ERROR and post it at https://github.com/GamerKing195/PluginUpdater/issues.");
        this.log.severe("");
        this.log.severe("============== BEGIN ERROR ==============");
        this.log.severe("PLUGIN VERSION: AutoUpdaterAPI V" + getDescription().getVersion());
        this.log.severe("");
        this.log.severe("MESSAGE: " + ex.getMessage());
        this.log.severe("");
        this.log.severe("STACKTRACE: ");
        ex.printStackTrace();
        this.log.severe("");
        this.log.severe("============== END ERROR ==============");
    }

    public void printError(Exception ex, String extraInfo)
    {
        this.log.severe("A severe error has occurred with AutoUpdaterAPI.");
        this.log.severe("If you cannot figure out this error on your own (e.g. a config error) please copy and paste everything from here to END ERROR and post it at https://github.com/GamerKing195/PluginUpdater/issues.");
        this.log.severe("");
        this.log.severe("============== BEGIN ERROR ==============");
        this.log.severe("PLUGIN VERSION: AutoUpdaterAPI V" + getDescription().getVersion());
        this.log.severe("");
        this.log.severe("PLUGIN MESSAGE: " + extraInfo);
        this.log.severe("");
        this.log.severe("MESSAGE: " + ex.getMessage());
        this.log.severe("");
        this.log.severe("STACKTRACE: ");
        ex.printStackTrace();
        this.log.severe("");
        this.log.severe("============== END ERROR ==============");
    }

    void printPluginError(String header, String message)
    {
        this.log.severe("============== BEGIN ERROR ==============");
        this.log.severe(header);
        this.log.severe("");
        this.log.severe("PLUGIN VERSION: AutoUpdaterAPI V" + getDescription().getVersion());
        this.log.severe("");
        this.log.severe("PLUGIN MESSAGE: " + message);
        this.log.severe("");
        this.log.severe("============== END ERROR ==============");
    }
}