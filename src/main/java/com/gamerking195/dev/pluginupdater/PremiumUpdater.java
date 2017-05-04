package com.gamerking195.dev.pluginupdater;

import be.maximvdw.spigotsite.SpigotSiteCore;
import be.maximvdw.spigotsite.api.SpigotSite;
import be.maximvdw.spigotsite.api.exceptions.ConnectionFailedException;
import be.maximvdw.spigotsite.api.user.User;
import be.maximvdw.spigotsite.api.user.exceptions.InvalidCredentialsException;
import be.maximvdw.spigotsite.api.user.exceptions.TwoFactorAuthenticationException;
import be.maximvdw.spigotsite.http.HTTPDownloadResponse;
import be.maximvdw.spigotsite.http.HTTPUnitRequest;
import be.maximvdw.spigotsite.user.SpigotUser;
import com.gamerking195.dev.pluginupdater.util.UtilPlugin;
import com.gamerking195.dev.pluginupdater.util.UtilSpigotCreds;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;

public class PremiumUpdater {
    private Player initiater;

    private JavaPlugin plugin;

    private String dataFolderPath;
    private String currentVersion;
    private String newVersion;
    private String pluginName;

    private User spigotUser;

    private UpdateLocale locale;

    private boolean delete;
    private int resourceId;

    public PremiumUpdater(Player initiater, JavaPlugin plugin, int resourceId, UpdateLocale locale, boolean delete) {
        dataFolderPath = Main.getInstance().getDataFolder().getPath();
        currentVersion = plugin.getDescription().getVersion();
        pluginName = plugin.getName();
        this.resourceId = resourceId;
        this.plugin = plugin;
        this.initiater = initiater;
        this.locale = locale;
        this.delete = delete;

        try {
            SpigotSite.getAPI().getResourceManager().getResourceById(resourceId).getLastVersion();
        } catch (Exception ex) {
            sendActionBar(initiater, locale.updateFailed.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion));
            Main.getInstance().printError(ex, "Error occured while loading PremiumUpdater");
            delete();
        }
    }

    public void update() {
        if (updateAvailable()) {
            try {

                if (spigotUser == null) {
                    authenticate();
                    sendActionBar(initiater, locale.updating.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion)+" &8[AUTHENTICATING]");
                }

                sendActionBar(initiater, locale.updating.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion)+" &8[RETREIVING FILES]");

                if (!new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).delete())
                    Main.getInstance().printPluginError("Error occurred while updating " + pluginName + ".", "Could not delete old plugin jar.");

                UtilPlugin.unload(plugin);

                HTTPDownloadResponse dlResponse = HTTPUnitRequest.downloadFile(
                        SpigotSite.getAPI().getResourceManager().getResourceById(resourceId).getDownloadURL(),
                        spigotUser != null ? ((SpigotUser) spigotUser).getCookies()
                                : SpigotSiteCore.getBaseCookies());
                long completeFileSize = dlResponse.getUrl().openConnection().getContentLength();

                BufferedInputStream in = new java.io.BufferedInputStream(dlResponse.getStream());
                java.io.FileOutputStream fos = new java.io.FileOutputStream(new File(dataFolderPath.substring(0, dataFolderPath.lastIndexOf("/")) + "/"+locale.fileName+".jar"));
                java.io.BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);

                byte[] data = new byte[1024];
                long downloadedFileSize = 0;
                int x = 0;
                while ((x = in.read(data, 0, 1024)) >= 0) {
                    downloadedFileSize += x;

                    final int currentProgress = (int) ((((double) downloadedFileSize) / ((double) completeFileSize)) * 15);

                    final String currentPercent = String.format("%.2f", (((double) downloadedFileSize) / ((double) completeFileSize)) * 100);

                    String bar = "&a:::::::::::::::";

                    bar = bar.substring(0, currentProgress + 2) + "&c" + bar.substring(currentProgress + 2);

                    sendActionBar(initiater, locale.updatingDownload.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion).replace("%download_bar%", bar).replace("%download_percent%", currentPercent+"%")+" &8[DOWNLOADING]");

                    bout.write(data, 0, x);
                }

                bout.close();
                in.close();

                sendActionBar(initiater, locale.updating.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion) + " &8[INITIALIZING]");

                Bukkit.getPluginManager().loadPlugin(new File(dataFolderPath.substring(0, dataFolderPath.lastIndexOf("/")) + "/"+locale.fileName+".jar"));
                Bukkit.getPluginManager().enablePlugin(Bukkit.getPluginManager().getPlugin(pluginName));

                sendActionBar(initiater, locale.updateComplete.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion));

                delete();
            } catch(Exception ex) {
                sendActionBar(initiater, locale.updateFailed.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion));
                Main.getInstance().printError(ex, "Error occurred while updating premium resource.");
                delete();
            }
        }
    }

    public boolean updateAvailable() {
        try {
            if (SpigotSite.getAPI().getResourceManager().getResourceById(resourceId) != null)
                return SpigotSite.getAPI().getResourceManager().getResourceById(resourceId).getLastVersion().equals(currentVersion);
        } catch(ConnectionFailedException ex) {
            sendActionBar(initiater, locale.updateFailed.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion));
            Main.getInstance().printError(ex, "Error occurred while connecting to spigot during update checking.");
            delete();
        }

        return false;
    }

    public void authenticate() {
        sendActionBar(initiater, locale.updating.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion)+" &8[ATTEMPTING DECRYPT]");

        String username = UtilSpigotCreds.getInstance().getUsername();
        String password = UtilSpigotCreds.getInstance().getPassword();
        String twoFactor = UtilSpigotCreds.getInstance().getTwoFactor();

        if (username == null || password == null || twoFactor == null) {
            runGuis();
        }

        try {
            sendActionBar(initiater, locale.updating.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion)+" &8[ATTEMPTING AUTHENTICATION]");
            spigotUser = SpigotSite.getAPI().getUserManager().authenticate(username, password);
        } catch (TwoFactorAuthenticationException ex) {
            try {
                sendActionBar(initiater, locale.updating.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion)+" &8[ATTEMPTING AUTHENTICATION]");
                spigotUser = SpigotSite.getAPI().getUserManager().authenticate(username, password, twoFactor);
            } catch (Exception exception) {
                runGuis();
            }
        } catch (InvalidCredentialsException ex) {
            runGuis();
        } catch (ConnectionFailedException ex) {
            sendActionBar(initiater, locale.updateFailed.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion));
            Main.getInstance().printError(ex, "Error occurred while connecting to spigot during authentication.");
            delete();
        }
    }

    private void runGuis() {
        sendActionBar(initiater, locale.updating.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion)+" &8[RETRIEVING USERNAME]");
        new AnvilGUI(Main.getInstance(), initiater, "Enter your Spigot username:", (Player player1, String usernameInput) -> {
            try {
                if (SpigotSite.getAPI().getUserManager().getUserByName(usernameInput) != null) {
                    sendActionBar(initiater, locale.updating.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion)+" &8[RETRIEVING PASSWORD]");
                    new AnvilGUI(Main.getInstance(), initiater, "Enter your Spigot password:", (Player player2, String passwordInput) -> {
                        try {
                            spigotUser = SpigotSite.getAPI().getUserManager().authenticate(usernameInput, passwordInput);

                            sendActionBar(initiater, locale.updating.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion)+" &8[ENCRYPTING CREDENTIALS]");
                            UtilSpigotCreds.getInstance().setUsername(usernameInput);
                            UtilSpigotCreds.getInstance().setPassword(passwordInput);
                        } catch (TwoFactorAuthenticationException ex) {
                            sendActionBar(initiater, locale.updating.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion)+" &8[RETRIEVING TWO FACTOR SECRET]");
                            new AnvilGUI(Main.getInstance(), initiater, "Enter your Spigot two factor secret:", (Player player3, String twoFactorInput) -> {
                                try {
                                    spigotUser = SpigotSite.getAPI().getUserManager().authenticate(usernameInput, passwordInput, twoFactorInput);

                                    sendActionBar(initiater, locale.updating.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion)+" &8[ENCRYPTING CREDENTIALS]");

                                    UtilSpigotCreds.getInstance().setUsername(usernameInput);
                                    UtilSpigotCreds.getInstance().setPassword(passwordInput);
                                    UtilSpigotCreds.getInstance().setTwoFactor(twoFactorInput);
                                    return "Authentication success";
                                } catch (Exception exception) {
                                    sendActionBar(initiater, locale.updateFailed.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion));
                                    Main.getInstance().printError(exception, "Error occurred while authenticating spigot user.");
                                    delete();
                                    return "Authentication failed";
                                }
                            });
                        } catch (ConnectionFailedException ex) {
                            sendActionBar(initiater, locale.updateFailed.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion));
                            Main.getInstance().printError(ex, "Error occured while authenticating spigot user");
                            delete();
                            return "Could not connect to Spigot";
                        } catch (InvalidCredentialsException ex) {
                            sendActionBar(initiater, locale.updateFailed.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion));
                            delete();
                            return "Invalid credentials";
                        }

                        return null;
                    });
                } else {
                    sendActionBar(initiater, locale.updateFailed.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion));
                    delete();
                    return "Invalid username!";
                }
            } catch (Exception ex) {
                Main.getInstance().printError(ex, "Error occured while authenticating spigot username.");
                sendActionBar(initiater, locale.updateFailed.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion));
                delete();
            }

            return null;
        });
    }

    /*
     * Utilities
     */
    private void sendActionBar(Player player, String message) {
        if (player != null)
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.translateAlternateColorCodes('&', message)));
    }

    private void delete() {
        if (delete) {
            try {
                if (!new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).delete())
                    Main.getInstance().printPluginError("Error occurred while updating " + pluginName + ".", "Could not delete updater jar.");

                UtilPlugin.unload(Main.getInstance());
            } catch (Exception ex) {
                Main.getInstance().printError(ex);
            }
        }
    }
}
