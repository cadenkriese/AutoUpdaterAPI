package com.gamerking195.dev.pluginupdater;

import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main
        extends JavaPlugin
{
    private static Main instance;
    private Logger log;

    static Main getInstance()
    {
        return instance;
    }

    public void onEnable()
    {
        instance = this;

        this.log = getLogger();
        this.log.info("Updater enabled!");
    }

    void printError(Exception ex)
    {
        this.log.severe("A severe error has occurred with the Thirst plugin.");
        this.log.severe("If you cannot figure out this error on your own (e.g. a config error) please copy and paste everything from here to END ERROR and post it at https://github.com/GamerKing195/Thirst/issues.");
        this.log.severe("");
        this.log.severe("============== BEGIN ERROR ==============");
        this.log.severe("PLUGIN VERSION: Thirst V" + getDescription().getVersion());
        this.log.severe("");
        this.log.severe("MESSAGE: " + ex.getMessage());
        this.log.severe("");
        this.log.severe("STACKTRACE: ");
        ex.printStackTrace();
        this.log.severe("");
        this.log.severe("============== END ERROR ==============");
    }

    void printError(Exception ex, String extraInfo)
    {
        this.log.severe("A severe error has occurred with the Thirst plugin.");
        this.log.severe("If you cannot figure out this error on your own (e.g. a config error) please copy and paste everything from here to END ERROR and post it at https://github.com/GamerKing195/Thirst/issues.");
        this.log.severe("");
        this.log.severe("============== BEGIN ERROR ==============");
        this.log.severe("PLUGIN VERSION: Thirst V" + getDescription().getVersion());
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
        this.log.severe("PLUGIN VERSION: Thirst V" + getDescription().getVersion());
        this.log.severe("");
        this.log.severe("PLUGIN MESSAGE: " + message);
        this.log.severe("");
        this.log.severe("============== END ERROR ==============");
    }
}