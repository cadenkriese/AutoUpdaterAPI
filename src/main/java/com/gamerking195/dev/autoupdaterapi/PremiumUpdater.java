package com.gamerking195.dev.autoupdaterapi;

import be.maximvdw.spigotsite.api.exceptions.ConnectionFailedException;
import be.maximvdw.spigotsite.api.resource.Resource;
import be.maximvdw.spigotsite.api.user.User;
import be.maximvdw.spigotsite.api.user.exceptions.InvalidCredentialsException;
import be.maximvdw.spigotsite.api.user.exceptions.TwoFactorAuthenticationException;
import be.maximvdw.spigotsite.user.SpigotUser;
import com.gamerking195.dev.autoupdaterapi.util.UtilPlugin;
import com.gamerking195.dev.autoupdaterapi.util.UtilSpigotCreds;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.Cookie;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.util.Map;

public class PremiumUpdater {
    private Player initiator;

    private JavaPlugin plugin;

    private String dataFolderPath;
    private String currentVersion;
    private String pluginName;

    private User spigotUser;
    private Resource resource;

    private UpdateLocale locale;

    private boolean delete;
    private int resourceId;

    public PremiumUpdater(Player initiator, JavaPlugin plugin, int resourceId, UpdateLocale locale, boolean delete) {
        spigotUser = Main.getInstance().getCurrentUser();
        dataFolderPath = Main.getInstance().getDataFolder().getPath();
        currentVersion = plugin.getDescription().getVersion();
        pluginName = locale.getFileName();
        this.resourceId = resourceId;
        this.plugin = plugin;
        this.initiator = initiator;
        this.locale = locale;
        this.delete = delete;
    }

    public void update() {
        try {
            sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[RETRIEVING PLUGIN INFO]");

            if (spigotUser == null) {
                authenticate();
                sendActionBar(initiator, locale.getUpdatingNoVar() + " &8[AUTHENTICATING SPIGOT ACCOUNT]");
                return;
            }

            for (Resource resource : Main.getInstance().getApi().getResourceManager().getPurchasedResources(spigotUser)) {
                if (resource.getResourceId() == resourceId) {
                    this.resource = resource;
                }
            }

            if (resource == null) {
                sendActionBar(initiator, "&c&lUPDATE FAILED &8[YOU HAVE NOT BOUGHT THAT PLUGIN]");
                delete();
                return;
            }

            String newVersion = Main.getInstance().getApi().getResourceManager().getResourceById(resourceId, spigotUser).getLastVersion();

            if (newVersion.equals(currentVersion)) {
                sendActionBar(initiator, locale.getUpdateFailed().replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion) + " &8[NO UPDATE AVAILABLE]");
                delete();
                return;
            }

            sendActionBar(initiator, locale.getUpdating().replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion) + " &8[RETRIEVING FILES]");

            if (!new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).delete())
                Main.getInstance().printPluginError("Error occurred while updating " + pluginName + ".", "Could not delete old plugin jar.");

            sendActionBar(initiator, locale.getUpdating().replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion) + " &8[ATTEMPTING DOWNLOAD]");

            Map<String, String> cookies = ((SpigotUser) spigotUser).getCookies();

            WebClient webClient = Main.getInstance().getWebClient();

            for (Map.Entry<String, String> entry : cookies.entrySet())
                webClient.getCookieManager().addCookie(new Cookie("spigotmc.org", entry.getKey(), entry.getValue()));

            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

            try {
                HtmlPage htmlPage = webClient.getPage(Main.getInstance().getApi().getResourceManager().getResourceById(resourceId, spigotUser).getDownloadURL());

                webClient.waitForBackgroundJavaScript(10_000);

                Integer completeFileSize = Integer.valueOf(htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getResponseHeaderValue("Content-Length"));

                BufferedInputStream in = new java.io.BufferedInputStream(htmlPage.getEnclosingWindow().getEnclosedPage().getWebResponse().getContentAsStream());
                java.io.FileOutputStream fos = new java.io.FileOutputStream(new File(dataFolderPath.substring(0, dataFolderPath.lastIndexOf("/")) + "/" + locale.getFileName() + ".jar"));
                java.io.BufferedOutputStream bout = new BufferedOutputStream(fos, 1024);

                byte[] data = new byte[1024];
                long downloadedFileSize = 0;
                int x;
                while ((x = in.read(data, 0, 1024)) >= 0) {
                    downloadedFileSize += x;

                    //Don't send action bar for every byte of data we're not trying to crash any clients here.
                    final int currentProgress = (int) ((((double) downloadedFileSize) / ((double) completeFileSize)) * 15);

                    final String currentPercent = String.format("%.2f", (((double) downloadedFileSize) / ((double) completeFileSize)) * 100);

                    String bar = "&a:::::::::::::::";

                    bar = bar.substring(0, currentProgress + 2) + "&c" + bar.substring(currentProgress + 2);

                    sendActionBar(initiator, locale.getUpdatingDownload().replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion).replace("%download_bar%", bar).replace("%download_percent%", currentPercent + "%") + " &8[DOWNLOADING RESOURCE]");

                    bout.write(data, 0, x);
                }

                bout.close();
                in.close();

                Main.getInstance().setWebClient(new WebClient(BrowserVersion.CHROME));

                UtilPlugin.unload(plugin);

                sendActionBar(initiator, locale.getUpdating().replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion) + " &8[INITIALIZING]");

                Bukkit.getPluginManager().loadPlugin(new File(dataFolderPath.substring(0, dataFolderPath.lastIndexOf("/")) + "/" + locale.getFileName() + ".jar"));
                Bukkit.getPluginManager().enablePlugin(Bukkit.getPluginManager().getPlugin(pluginName));

                sendActionBar(initiator, locale.getUpdateComplete().replace("%plugin%", pluginName).replace("%old_version%", currentVersion).replace("%new_version%", newVersion));

                delete();
            } catch (Exception ex) {
                sendActionBar(initiator, locale.getUpdateFailedNoVar());
                Main.getInstance().printError(ex, "Error occurred while updating premium resource.");
                delete();
            }
        } catch(Exception ex) {
            sendActionBar(initiator, locale.getUpdateFailedNoVar());
            Main.getInstance().printError(ex, "Error occurred while updating premium resource.");
            delete();
        }
    }

    private void authenticate() {
        sendActionBar(initiator, locale.getUpdatingNoVar()+" &8[ATTEMPTING DECRYPT]");

        String username = UtilSpigotCreds.getInstance().getUsername();
        String password = UtilSpigotCreds.getInstance().getPassword();
        String twoFactor = UtilSpigotCreds.getInstance().getTwoFactor();

        if (username == null || password == null) {
            runGuis();
            return;
        }

        try {
            sendActionBar(initiator, locale.getUpdatingNoVar()+" &8[ATTEMPTING AUTHENTICATION]");
            spigotUser = Main.getInstance().getApi().getUserManager().authenticate(username, password);
            Main.getInstance().setCurrentUser(spigotUser);
        } catch (TwoFactorAuthenticationException ex) {
            try {
                sendActionBar(initiator, locale.getUpdatingNoVar()+" &8[RE-ATTEMPTING AUTHENTICATION]");
                if (twoFactor == null) {
                    runGuis();
                    return;
                }

                spigotUser = Main.getInstance().getApi().getUserManager().authenticate(username, password, twoFactor);
                Main.getInstance().setCurrentUser(spigotUser);
            } catch (Exception exception) {
                runGuis();
                return;
            }
        } catch (InvalidCredentialsException ex) {
            sendActionBar(initiator, locale.getUpdatingNoVar()+" &c[INVALID CACHED CREDENTIALS]");
            runGuis();
            return;
        } catch (ConnectionFailedException ex) {
            sendActionBar(initiator, locale.getUpdateFailedNoVar());
            Main.getInstance().printError(ex, "Error occurred while connecting to spigot during authentication.");
            delete();
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    update();
                } catch(Exception ex) {
                    Main.getInstance().printError(ex);
                }
            }
        }.runTaskLater(Main.getInstance(), 40L);
    }

    private void runGuis() {
        sendActionBar(initiator, locale.getUpdatingNoVar()+" &8[RETRIEVING USERNAME]");
        new AnvilGUI(Main.getInstance(), initiator, "Spigot username", (Player player1, String usernameInput) -> {
            try {
                if (Main.getInstance().getApi().getUserManager().getUserByName(usernameInput) != null) {
                    sendActionBar(initiator, locale.getUpdatingNoVar()+" &8[RETRIEVING PASSWORD]");
                    new AnvilGUI(Main.getInstance(), initiator, "Spigot password", (Player player2, String passwordInput) -> {
                        try {
                            spigotUser = Main.getInstance().getApi().getUserManager().authenticate(usernameInput, passwordInput);

                            sendActionBar(initiator, locale.getUpdatingNoVar()+" &8[ENCRYPTING CREDENTIALS]");
                            UtilSpigotCreds.getInstance().setUsername(usernameInput);
                            UtilSpigotCreds.getInstance().setPassword(passwordInput);
                            UtilSpigotCreds.getInstance().saveFile();

                            authenticate();
                            player2.closeInventory();

                        } catch (TwoFactorAuthenticationException ex) {
                            sendActionBar(initiator, locale.getUpdatingNoVar()+" &8[RETRIEVING TWO FACTOR SECRET]");
                            new AnvilGUI(Main.getInstance(), initiator, "Spigot two factor secret", (Player player3, String twoFactorInput) -> {
                                try {
                                    //Make extra string because the input seems to change for some reason.
                                    final String twoFactorSecret = twoFactorInput;
                                    spigotUser = Main.getInstance().getApi().getUserManager().authenticate(usernameInput, passwordInput, twoFactorSecret);

                                    sendActionBar(initiator, locale.getUpdatingNoVar()+" &8[ENCRYPTING CREDENTIALS]");

                                    UtilSpigotCreds.getInstance().setUsername(usernameInput);
                                    UtilSpigotCreds.getInstance().setPassword(passwordInput);
                                    UtilSpigotCreds.getInstance().setTwoFactor(twoFactorSecret);
                                    UtilSpigotCreds.getInstance().saveFile();

                                    authenticate();
                                    player3.closeInventory();

                                    return "Retrieved credentials you may now close this GUI.";
                                } catch (Exception exception) {
                                    sendActionBar(initiator, locale.getUpdateFailedNoVar());
                                    Main.getInstance().printError(exception, "Error occurred while authenticating Spigot user.");
                                    delete();
                                    return "Authentication failed";
                                }
                            });
                        } catch (ConnectionFailedException ex) {
                            sendActionBar(initiator, locale.getUpdateFailedNoVar());
                            Main.getInstance().printError(ex, "Error occurred while authenticating spigot user");
                            delete();
                            return "Could not connect to Spigot";
                        } catch (InvalidCredentialsException ex) {
                            sendActionBar(initiator, locale.getUpdateFailedNoVar());
                            delete();
                            return "Invalid credentials";
                        }

                        return null;
                    });
                } else {
                    sendActionBar(initiator, locale.getUpdateFailedNoVar());
                    delete();
                    return "Invalid username!";
                }
            } catch (Exception ex) {
                Main.getInstance().printError(ex, "Error occurred while authenticating Spigot username.");
                sendActionBar(initiator, locale.getUpdateFailedNoVar());
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
        if (Main.getInstance().isDebug())
            Main.getInstance().getLogger().info(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', message)));
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