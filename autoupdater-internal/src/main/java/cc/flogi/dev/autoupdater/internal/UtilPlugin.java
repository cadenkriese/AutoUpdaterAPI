package cc.flogi.dev.autoupdater.internal;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

public final class UtilPlugin {
    /**
     * Method is from PlugMan, developed by Ryan Clancy "rylinaux"
     *
     * Copyright (c) 2014 Ryan Clancy
     *
     * Licensed under the MIT License
     *
     * PlugMan https://dev.bukkit.org/projects/plugman
     *
     * @param plugin The plugin that needs to be unloaded.
     */
    public static void unload(Plugin plugin) {
        String name = plugin.getName();
        PluginManager pluginManager = Bukkit.getPluginManager();
        SimpleCommandMap commandMap = null;
        List<Plugin> plugins = null;
        Map<String, Plugin> names = null;
        Map<String, Command> commands = null;
        Map<Event, SortedSet<RegisteredListener>> listeners = null;
        boolean reloadlisteners = true;

        if (pluginManager != null) {
            pluginManager.disablePlugin(plugin);
            try {
                Field pluginsField = Bukkit.getPluginManager().getClass().getDeclaredField("plugins");
                pluginsField.setAccessible(true);
                plugins = (List<Plugin>) pluginsField.get(pluginManager);

                Field lookupNamesField = Bukkit.getPluginManager().getClass().getDeclaredField("lookupNames");
                lookupNamesField.setAccessible(true);
                names = (Map<String, Plugin>) lookupNamesField.get(pluginManager);

                try {
                    Field listenersField = Bukkit.getPluginManager().getClass().getDeclaredField("listeners");
                    listenersField.setAccessible(true);
                    listeners = (Map<Event, SortedSet<RegisteredListener>>) listenersField.get(pluginManager);
                } catch (Exception e) {
                    reloadlisteners = false;
                }
                Field commandMapField = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
                commandMapField.setAccessible(true);
                commandMap = (SimpleCommandMap) commandMapField.get(pluginManager);

                Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
                knownCommandsField.setAccessible(true);
                commands = (Map<String, Command>) knownCommandsField.get(commandMap);
            } catch (NoSuchFieldException | IllegalAccessException ex) {
                AutoUpdaterInternal.get().printError(ex);
            }
        }

        pluginManager.disablePlugin(plugin);

        if (plugins != null)
            plugins.remove(plugin);
        if (names != null)
            names.remove(name);

        if (listeners != null && reloadlisteners) {
            for (SortedSet<RegisteredListener> set : listeners.values()) {
                set.removeIf(value -> value.getPlugin() == plugin);
            }
        }

        if (commandMap != null) {
            for (Iterator<Map.Entry<String, Command>> it = commands.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, Command> entry = it.next();
                if (entry.getValue() instanceof PluginCommand) {
                    PluginCommand cmd = (PluginCommand) entry.getValue();
                    if (cmd.getPlugin() == plugin) {
                        cmd.unregister(commandMap);
                        it.remove();
                    }
                }
            }
        }

        // Attempt to close the classloader to unlock any handles on the plugin's jar file.
        ClassLoader loader = plugin.getClass().getClassLoader();
        if (loader instanceof URLClassLoader) {
            try {
                Field pluginField = loader.getClass().getDeclaredField("plugin");
                pluginField.setAccessible(true);
                pluginField.set(loader, null);

                Field pluginInitField = loader.getClass().getDeclaredField("pluginInit");
                pluginInitField.setAccessible(true);
                pluginInitField.set(loader, null);
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                AutoUpdaterInternal.get().printError(ex);
            }

            try {
                /* TODO
                 * This causes major issues when closing your own classloader.
                 * Try to figure out a cleaner way to do this.
                 */
                if (!plugin.getName().equalsIgnoreCase("autoupdater-plugin"))
                    ((URLClassLoader) loader).close();
            } catch (IOException ex) {
                AutoUpdaterInternal.get().printError(ex);
            }
        }

        // Will not work on processes started with the -XX:+DisableExplicitGC flag, but let's try it anyway.
        // This tries to get around the issue where Windows refuses to unlock jar files that were previously loaded into the JVM.
        System.gc();
    }

    public static Plugin loadPlugin(File file) throws InvalidDescriptionException, InvalidPluginException {
        Plugin plugin = AutoUpdaterInternal.getPlugin().getServer().getPluginManager().loadPlugin(file);
        plugin.onLoad();
        AutoUpdaterInternal.getPlugin().getServer().getPluginManager().enablePlugin(plugin);
        return plugin;
    }
}