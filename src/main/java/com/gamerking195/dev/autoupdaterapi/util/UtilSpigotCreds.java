package com.gamerking195.dev.autoupdaterapi.util;

/*
 * Created by Caden Kriese (flogic) on 6/14/17.
 *
 * License is specified by the distributor which this
 * file was written for. Otherwise it can be found in the LICENSE file.
 */

import be.maximvdw.spigotsite.api.SpigotSiteAPI;
import com.gamerking195.dev.autoupdaterapi.AutoUpdaterAPI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;

public class UtilSpigotCreds {
    private UtilSpigotCreds() {
    }

    private static UtilSpigotCreds instance = new UtilSpigotCreds();

    public static UtilSpigotCreds getInstance() {
        return instance;
    }

    private File infoFile;
    private FileConfiguration infoConfig;

    public void init() {
        infoFile = new File(AutoUpdaterAPI.getInstance().getDataFolder().getParentFile().getAbsolutePath() + AutoUpdaterAPI.getFileSeperator() + ".creds" + AutoUpdaterAPI.getFileSeperator() + "info.enc");
        if (!infoFile.getParentFile().exists())
            infoFile.getParentFile().mkdirs();
        infoConfig = YamlConfiguration.loadConfiguration(infoFile);

        infoConfig.options().copyDefaults(true);

        if (!infoFile.exists()) {
            try {
                infoFile.createNewFile();
                infoConfig.save(infoFile);
            } catch (IOException ex) {
                AutoUpdaterAPI.getInstance().printError(ex, "Error occurred while creating the encrypted file.");
            }
        }

        UtilEncryption.getInstance().init();
    }

    public void updateKeys() {
        try {
            UtilEncryption.getInstance().useOldKeys = true;
            SpigotSiteAPI api = AutoUpdaterAPI.getInstance().getApi();

            UtilEncryption.getInstance().setKeyNumber(3);
            if (infoConfig.contains(UtilEncryption.getInstance().encrypt("username"))) {
                String username = getUsername();

                if (api.getUserManager().getUserByName(username) != null) {

                    String pass = getPassword();
                    String twofactor = getTwoFactor();

                    reset();
                    init();

                    UtilEncryption.getInstance().useOldKeys = false;

                    setUsername(username);
                    setPassword(pass);
                    setTwoFactor(twofactor);

                    saveFile();

                    AutoUpdaterAPI.getInstance().getLogger().info("Successfully converted to new encryption keys.");
                }
            } else {
                UtilEncryption.getInstance().useOldKeys = false;
                AutoUpdaterAPI.getInstance().getLogger().info("Updated keys detected and enabled.");
            }
        } catch (Exception e) {
            AutoUpdaterAPI.getInstance().printError(e);
        }
    }

    public void reset() {
        if (infoFile.delete() && infoFile.getParentFile().delete()) {
            infoFile = null;
            infoConfig = null;

            init();
        } else
            AutoUpdaterAPI.getInstance().printPluginError("Error occurred while resetting credentials.", "Info file deletion failed.");
    }

    public void saveFile() {
        try {
            infoConfig.save(infoFile);
        } catch (IOException ex) {
            AutoUpdaterAPI.getInstance().printError(ex, "Error occurred while creating the encrypted file.");
        }
    }

    public void clearFile() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    infoConfig.set(UtilEncryption.getInstance().encrypt("username"), null);
                    infoConfig.set(UtilEncryption.getInstance().encrypt("password"), null);
                    infoConfig.set(UtilEncryption.getInstance().encrypt("twoFactorSecret"), null);
                } catch (Exception e) {
                    AutoUpdaterAPI.getInstance().printError(e);
                }

                saveFile();
            }
        }.runTask(AutoUpdaterAPI.getInstance());
    }


    /*
     * GETTERS
     */

    public String getUsername() {
        UtilEncryption.getInstance().setKeyNumber(3);

        String key = UtilEncryption.getInstance().encrypt("username");

        UtilEncryption.getInstance().setKeyNumber(0);

        if (infoConfig.getString(key) != null) {
            return UtilEncryption.getInstance().decrypt(infoConfig.getString(key));
        }

        return null;
    }

    public String getPassword() {
        UtilEncryption.getInstance().setKeyNumber(3);

        String key = UtilEncryption.getInstance().encrypt("password");

        UtilEncryption.getInstance().setKeyNumber(1);

        if (infoConfig.getString(key) != null) {
            return UtilEncryption.getInstance().decrypt(infoConfig.getString(key));
        }

        return null;
    }

    public String getTwoFactor() {
        UtilEncryption.getInstance().setKeyNumber(3);

        String key = UtilEncryption.getInstance().encrypt("twoFactorSecret");

        UtilEncryption.getInstance().setKeyNumber(2);

        if (infoConfig.getString(key) != null) {
            return UtilEncryption.getInstance().decrypt(infoConfig.getString(key));
        }

        return null;
    }

    /*
     * SETTERS
     */

    public void setUsername(String username) {
        UtilEncryption.getInstance().setKeyNumber(3);

        String key;
        key = UtilEncryption.getInstance().encrypt("username");

        UtilEncryption.getInstance().setKeyNumber(0);

        infoConfig.set(key, UtilEncryption.getInstance().encrypt(username));
    }

    public void setPassword(String password) {
        UtilEncryption.getInstance().setKeyNumber(3);

        String key = UtilEncryption.getInstance().encrypt("password");

        UtilEncryption.getInstance().setKeyNumber(1);

        infoConfig.set(key, UtilEncryption.getInstance().encrypt(password));
    }

    public void setTwoFactor(String twoFactor) {
        UtilEncryption.getInstance().setKeyNumber(3);

        String key = UtilEncryption.getInstance().encrypt("twoFactorSecret");

        UtilEncryption.getInstance().setKeyNumber(2);

        infoConfig.set(key, UtilEncryption.getInstance().encrypt(twoFactor));
    }
}
