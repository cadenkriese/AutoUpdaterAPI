package cc.flogi.dev.autoupdater.util;

import cc.flogi.dev.autoupdater.AutoUpdaterAPI;
import cc.flogi.dev.autoupdater.PremiumController;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 6/14/17
 */
@SuppressWarnings("ResultOfMethodCallIgnored") public class UtilSpigotCreds {
    private static UtilSpigotCreds instance = new UtilSpigotCreds();
    private File infoFile;
    private FileConfiguration infoConfig;

    private UtilSpigotCreds() {}

    public static UtilSpigotCreds get() {
        return instance;
    }

    public void init() {
        infoFile = new File(PremiumController.get().getPrivateDataFolder().getAbsolutePath() + "/.creds/info.enc");
        if (!infoFile.getParentFile().exists())
            infoFile.getParentFile().mkdirs();
        infoConfig = YamlConfiguration.loadConfiguration(infoFile);

        infoConfig.options().copyDefaults(true);

        if (!infoFile.exists()) {
            try {
                infoFile.createNewFile();
                infoConfig.save(infoFile);
            } catch (IOException ex) {
                AutoUpdaterAPI.get().printError(ex, "Error occurred while creating the encrypted file.");
            }
        }

        UtilEncryption.getInstance().init();
    }

    public void reset() {
        if (infoFile.delete() && infoFile.getParentFile().delete()) {
            infoFile = null;
            infoConfig = null;

            init();
        } else
            AutoUpdaterAPI.get().printPluginError("Error occurred while resetting credentials.", "Info file deletion failed.");
    }

    public void saveFile() {
        try {
            infoConfig.save(infoFile);
        } catch (IOException ex) {
            AutoUpdaterAPI.get().printError(ex, "Error occurred while creating the encrypted file.");
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
                    AutoUpdaterAPI.get().printError(e);
                }

                saveFile();
            }
        }.runTask(AutoUpdaterAPI.getPlugin());
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

    public void setUsername(String username) {
        UtilEncryption.getInstance().setKeyNumber(3);

        String key;
        key = UtilEncryption.getInstance().encrypt("username");

        UtilEncryption.getInstance().setKeyNumber(0);

        infoConfig.set(key, UtilEncryption.getInstance().encrypt(username));
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

    /*
     * SETTERS
     */

    public void setPassword(String password) {
        UtilEncryption.getInstance().setKeyNumber(3);

        String key = UtilEncryption.getInstance().encrypt("password");

        UtilEncryption.getInstance().setKeyNumber(1);

        infoConfig.set(key, UtilEncryption.getInstance().encrypt(password));
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

    public void setTwoFactor(String twoFactor) {
        UtilEncryption.getInstance().setKeyNumber(3);

        String key = UtilEncryption.getInstance().encrypt("twoFactorSecret");

        UtilEncryption.getInstance().setKeyNumber(2);

        infoConfig.set(key, UtilEncryption.getInstance().encrypt(twoFactor));
    }
}
