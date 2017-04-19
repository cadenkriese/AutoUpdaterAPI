package com.gamerking195.dev.pluginupdater;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;


public final class Main extends JavaPlugin {

    private static Main instance;
    static Main getInstance() {
        return instance;
    }

    private Logger log;

    @Override
    public void onEnable() {
        instance = this;

        log = getLogger();
        log.info("Updater enabled!");

    }

    public void printError(Exception ex) {
        log.severe("A severe error has occured with the Thirst plugin.");
        log.severe("If you cannot figure out this error on your own (e.g. a config error) please copy and paste everything from here to END ERROR and post it at https://github.com/GamerKing195/Thirst/issues.");
        log.severe("");
        log.severe("============== BEGIN ERROR ==============");
        log.severe("PLUGIN VERSION: Thirst V" + getDescription().getVersion());
        log.severe("");
        log.severe("MESSAGE: " + ex.getMessage());
        log.severe("");
        log.severe("STACKTRACE: ");
        ex.printStackTrace();
        log.severe("");
        log.severe("============== END ERROR ==============");
    }

    public void printError(Exception ex, String extraInfo) {
        log.severe("A severe error has occured with the Thirst plugin.");
        log.severe("If you cannot figure out this error on your own (e.g. a config error) please copy and paste everything from here to END ERROR and post it at https://github.com/GamerKing195/Thirst/issues.");
        log.severe("");
        log.severe("============== BEGIN ERROR ==============");
        log.severe("PLUGIN VERSION: Thirst V" + getDescription().getVersion());
        log.severe("");
        log.severe("PLUGIN MESSAGE: "+extraInfo);
        log.severe("");
        log.severe("MESSAGE: " + ex.getMessage());
        log.severe("");
        log.severe("STACKTRACE: ");
        ex.printStackTrace();
        log.severe("");
        log.severe("============== END ERROR ==============");
    }

    public void printPluginError(String header, String message) {
        log.severe("============== BEGIN ERROR ==============");
        log.severe(header);
        log.severe("");
        log.severe("PLUGIN VERSION: Thirst V" + getDescription().getVersion());
        log.severe("");
        log.severe("PLUGIN MESSAGE: "+message);
        log.severe("");
        log.severe("============== END ERROR ==============");
    }
}
