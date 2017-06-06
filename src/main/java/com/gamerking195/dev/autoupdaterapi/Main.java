package com.gamerking195.dev.autoupdaterapi;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import be.maximvdw.spigotsite.SpigotSiteCore;
import be.maximvdw.spigotsite.api.SpigotSiteAPI;
import com.gamerking195.dev.autoupdaterapi.util.UtilDownloader;
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

    private SpigotSiteAPI api;

    public void onEnable()
    {
        instance = this;
        log = getLogger();

        UtilDownloader.downloadLib(UtilDownloader.Library.HTMMLUNIT);

        try {
            LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log",
                    "org.apache.commons.logging.impl.NoOpLog");

            java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
        } catch (Exception ex) {
            printError(ex, "Unable to turn off HTML unit logging!.");
        }

        log.info("Initializing connection with spigot, this may take a while...");

        try {
            api = new SpigotSiteCore();
        } catch (Exception ex) {
            printError(ex, "Error occurred while initializing the spigot site API.");
            return;
        }

        getConfig().options().copyHeader(true);

        try {
            new File(getDataFolder().getPath() + "/info.yml").getParentFile().mkdirs();
            new File(getDataFolder().getPath() + "/info.yml").createNewFile();

            getConfig().load(new File(getDataFolder().getPath() + "/info.yml"));
        } catch (Exception ex) {
            printError(ex, "Error occured while loading config.");
        }

        log.info("Updater enabled!");
    }

    SpigotSiteAPI getApi() {
        return api;
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