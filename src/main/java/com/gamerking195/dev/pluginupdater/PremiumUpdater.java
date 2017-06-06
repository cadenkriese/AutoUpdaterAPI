package com.gamerking195.dev.pluginupdater;

import be.maximvdw.spigotsite.api.exceptions.ConnectionFailedException;
import be.maximvdw.spigotsite.api.resource.Resource;
import be.maximvdw.spigotsite.api.user.User;
import be.maximvdw.spigotsite.api.user.exceptions.InvalidCredentialsException;
import be.maximvdw.spigotsite.api.user.exceptions.TwoFactorAuthenticationException;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class PremiumUpdater {
    private Player initiater;

    private JavaPlugin plugin;

    private String dataFolderPath;
    private String currentVersion;
    private String newVersion;
    private String pluginName;

    private User spigotUser;
    private Resource resource;

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
    }

    public void update() {
        try {
            if (spigotUser == null) {
                authenticate();
                sendActionBar(initiater, locale.updatingNoVar+" &8[AUTHENTICATING SPIGOT ACCOUNT]");
            }

//            for (Resource res : spigotUser.getPurchasedResources()) {
//                if (res.getResourceId() == resourceId) {
//                    resource = res;
//                    newVersion = resource.getLastVersion();
//
//                    if (newVersion.equals(currentVersion)) {
//                        sendActionBar(initiater, locale.updateFailed.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion)+" &8[NO UPDATE AVAILABLE]");
//                        delete();
//                        return;
//                    }
//                }
//            }


            for (Resource resource : Main.getInstance().getApi().getResourceManager().getPurchasedResources(spigotUser)) {
                if (resource.getResourceId() == resourceId) {
                    this.resource = resource;
                }
            }

            if (resource == null) {
                sendActionBar(initiater, locale.updateFailedNoVar+" &8[COULD NOT FIND RESOURCE]");
                delete();
                return;
            }

            newVersion = resource.getLastVersion();

            if (newVersion.equals(currentVersion)) {
                sendActionBar(initiater, locale.updateFailed.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion)+" &8[NO UPDATE AVAILABLE]");
                delete();
                return;
            }

            sendActionBar(initiater, locale.updating.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion)+" &8[RETREIVING FILES]");

            if (!new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).delete())
                Main.getInstance().printPluginError("Error occurred while updating " + pluginName + ".", "Could not delete old plugin jar.");

            Bukkit.broadcastMessage("RESOURCE INFO \n NAME: "+resource.getResourceName()+ " \n ID: "+resource.getResourceId()+" \n URL: "+ resource.getDownloadURL());

            resource.downloadResource(spigotUser, new File(dataFolderPath.substring(0, dataFolderPath.lastIndexOf("/")) + "/"+locale.fileName+".jar"));

            HttpURLConnection httpConnection = (HttpURLConnection) new URL(Main.getInstance().getApi().getResourceManager().getResourceById(resourceId, spigotUser).getDownloadURL()).openConnection();

            Map<String, String> cookies = ((SpigotUser) spigotUser).getCookies();

            StringBuilder sb = new StringBuilder();

            for (String string : cookies.keySet()) {
                sb.append(string).append("=").append(cookies.get(string)).append("; ");
            }

            String cookieString = sb.toString();

            cookieString = cookieString.substring(0, cookieString.length()-2);

            Bukkit.broadcastMessage(cookieString);

            httpConnection.setRequestProperty("Cookie", cookieString);

            httpConnection.connect();

            long completeFileSize = httpConnection.getContentLength();

            BufferedInputStream in = new java.io.BufferedInputStream(httpConnection.getInputStream());
            java.io.FileOutputStream fos = new java.io.FileOutputStream(new File(dataFolderPath.substring(0, dataFolderPath.lastIndexOf("/")) + "/"+locale.fileName+".jar"));
            java.io.BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);

            byte[] data = new byte[1024];
            long downloadedFileSize = 0;
            int x = 0;
            while ((x = in.read(data, 0, 1024)) >= 0) {
                downloadedFileSize += x;

                //Don't send action bar for every byte of data we're not trying to crash any clients here.
                if (x % 500 == 0) {
                    final int currentProgress = (int) ((((double) downloadedFileSize) / ((double) completeFileSize)) * 15);

                    final String currentPercent = String.format("%.2f", (((double) downloadedFileSize) / ((double) completeFileSize)) * 100);

                    String bar = "&a:::::::::::::::";

                    bar = bar.substring(0, currentProgress + 2) + "&c" + bar.substring(currentProgress + 2);

                    sendActionBar(initiater, locale.updatingDownload.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion).replace("%download_bar%", bar).replace("%download_percent%", currentPercent + "%") + " &8[DOWNLOADING]");
                }

                bout.write(data, 0, x);
            }

            bout.close();
            in.close();

            UtilPlugin.unload(plugin);

            sendActionBar(initiater, locale.updating.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion) + " &8[INITIALIZING]");

            Bukkit.getPluginManager().loadPlugin(new File(dataFolderPath.substring(0, dataFolderPath.lastIndexOf("/")) + "/"+locale.fileName+".jar"));
            Bukkit.getPluginManager().enablePlugin(Bukkit.getPluginManager().getPlugin(pluginName));

            sendActionBar(initiater, locale.updateComplete.replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion));

            delete();
        } catch(Exception ex) {
            sendActionBar(initiater, locale.updateFailedNoVar);
            Main.getInstance().printError(ex, "Error occurred while updating premium resource.");
            delete();
        }
    }

    public void authenticate() {
        sendActionBar(initiater, locale.updatingNoVar+" &8[ATTEMPTING DECRYPT]");

        String username = UtilSpigotCreds.getInstance().getUsername();
        String password = UtilSpigotCreds.getInstance().getPassword();
        String twoFactor = UtilSpigotCreds.getInstance().getTwoFactor();

        if (username == null || password == null) {
            runGuis();
        }

        try {
            sendActionBar(initiater, locale.updatingNoVar+" &8[ATTEMPTING AUTHENTICATION]");
            spigotUser = Main.getInstance().getApi().getUserManager().authenticate(username, password);
        } catch (TwoFactorAuthenticationException ex) {
            try {
                sendActionBar(initiater, locale.updatingNoVar+" &8[ATTEMPTING AUTHENTICATION]");
                if (twoFactor == null)
                    runGuis();

                Bukkit.broadcastMessage("#1 = " + twoFactor);

                spigotUser = Main.getInstance().getApi().getUserManager().authenticate(username, password, twoFactor);
            } catch (Exception exception) {
                runGuis();
            }
        } catch (InvalidCredentialsException ex) {
            runGuis();
        } catch (ConnectionFailedException ex) {
            sendActionBar(initiater, locale.updateFailedNoVar);
            Main.getInstance().printError(ex, "Error occurred while connecting to spigot during authentication.");
            delete();
        }
    }

    private void runGuis() {
        sendActionBar(initiater, locale.updatingNoVar+" &8[RETRIEVING USERNAME]");
        new AnvilGUI(Main.getInstance(), initiater, "Spigot username", (Player player1, String usernameInput) -> {
            try {
                if (Main.getInstance().getApi().getUserManager().getUserByName(usernameInput) != null) {
                    sendActionBar(initiater, locale.updatingNoVar+" &8[RETRIEVING PASSWORD]");
                    new AnvilGUI(Main.getInstance(), initiater, "Spigot password", (Player player2, String passwordInput) -> {
                        try {
                            spigotUser = Main.getInstance().getApi().getUserManager().authenticate(usernameInput, passwordInput);

                            sendActionBar(initiater, locale.updatingNoVar+" &8[ENCRYPTING CREDENTIALS]");
                            UtilSpigotCreds.getInstance().setUsername(usernameInput);
                            UtilSpigotCreds.getInstance().setPassword(passwordInput);
                            UtilSpigotCreds.getInstance().saveConfig();

                            authenticate();
                            player2.closeInventory();
                        } catch (TwoFactorAuthenticationException ex) {
                            sendActionBar(initiater, locale.updatingNoVar+" &8[RETRIEVING TWO FACTOR SECRET]");
                            new AnvilGUI(Main.getInstance(), initiater, "Spigot two factor secret", (Player player3, String twoFactorInput) -> {
                                try {
                                    final String twoFactorSecret = twoFactorInput;
                                    Bukkit.broadcastMessage("#2 = " +  twoFactorSecret);
                                    spigotUser = Main.getInstance().getApi().getUserManager().authenticate(usernameInput, passwordInput, twoFactorSecret);

                                    sendActionBar(initiater, locale.updatingNoVar+" &8[ENCRYPTING CREDENTIALS]");

                                    UtilSpigotCreds.getInstance().setUsername(usernameInput);
                                    UtilSpigotCreds.getInstance().setPassword(passwordInput);
                                    UtilSpigotCreds.getInstance().setTwoFactor(twoFactorSecret);
                                    UtilSpigotCreds.getInstance().saveConfig();

                                    authenticate();
                                    player3.closeInventory();
                                    return "Retrieved credentials you may now close this GUI.";
                                } catch (Exception exception) {
                                    sendActionBar(initiater, locale.updateFailedNoVar);
                                    Main.getInstance().printError(exception, "Error occurred while authenticating spigot user.");
                                    delete();
                                    return "Authentication failed";
                                }
                            });
                        } catch (ConnectionFailedException ex) {
                            sendActionBar(initiater, locale.updateFailedNoVar);
                            Main.getInstance().printError(ex, "Error occured while authenticating spigot user");
                            delete();
                            return "Could not connect to Spigot";
                        } catch (InvalidCredentialsException ex) {
                            sendActionBar(initiater, locale.updateFailedNoVar);
                            delete();
                            return "Invalid credentials";
                        }

                        return null;
                    });
                } else {
                    sendActionBar(initiater, locale.updateFailedNoVar);
                    delete();
                    return "Invalid username!";
                }
            } catch (Exception ex) {
                Main.getInstance().printError(ex, "Error occured while authenticating spigot username.");
                sendActionBar(initiater, locale.updateFailedNoVar);
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
