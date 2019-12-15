package cc.flogi.dev.autoupdater;

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
final class UtilSpigotCreds {
    private static File infoFile;
    private static FileConfiguration infoConfig;

    protected static void init() {
        infoFile = new File(InternalCore.getDataFolder().getAbsolutePath() + "/.creds/info.enc");
        if (!infoFile.getParentFile().exists())
            infoFile.getParentFile().mkdirs();
        infoConfig = YamlConfiguration.loadConfiguration(infoFile);

        infoConfig.options().copyDefaults(true);

        if (!infoFile.exists()) {
            try {
                infoFile.createNewFile();
                infoConfig.save(infoFile);
            } catch (IOException ex) {
                InternalCore.get().printError(ex, "Error occurred while creating the encrypted file.");
            }
        }
    }

    protected static void reset() {
        if (infoFile.delete() && infoFile.getParentFile().delete()) {
            infoFile = null;
            infoConfig = null;

            init();
        } else
            InternalCore.get().printPluginError("Error occurred while resetting credentials.", "Info file deletion failed.");
    }

    protected static void saveFile() {
        try {
            infoConfig.save(infoFile);
        } catch (IOException ex) {
            InternalCore.get().printError(ex, "Error occurred while creating the encrypted file.");
        }
    }

    protected static void clearFile() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    infoConfig.set(UtilEncryption.encrypt("username"), null);
                    infoConfig.set(UtilEncryption.encrypt("password"), null);
                    infoConfig.set(UtilEncryption.encrypt("twoFactorSecret"), null);
                } catch (Exception ex) {
                    InternalCore.get().printError(ex);
                }

                saveFile();
            }
        }.runTask(InternalCore.getPlugin());
    }


    /*
     * GETTERS
     */

    protected static String getUsername() {
        UtilEncryption.setKeyNumber(3);

        String key = UtilEncryption.encrypt("username");

        UtilEncryption.setKeyNumber(0);

        if (infoConfig.getString(key) != null) {
            return UtilEncryption.decrypt(infoConfig.getString(key));
        }

        return null;
    }

    protected static void setUsername(String username) {
        UtilEncryption.setKeyNumber(3);

        String key;
        key = UtilEncryption.encrypt("username");

        UtilEncryption.setKeyNumber(0);

        infoConfig.set(key, UtilEncryption.encrypt(username));
    }

    protected static String getPassword() {
        UtilEncryption.setKeyNumber(3);

        String key = UtilEncryption.encrypt("password");

        UtilEncryption.setKeyNumber(1);

        if (infoConfig.getString(key) != null) {
            return UtilEncryption.decrypt(infoConfig.getString(key));
        }

        return null;
    }

    /*
     * SETTERS
     */

    protected static void setPassword(String password) {
        UtilEncryption.setKeyNumber(3);

        String key = UtilEncryption.encrypt("password");

        UtilEncryption.setKeyNumber(1);

        infoConfig.set(key, UtilEncryption.encrypt(password));
    }

    protected static String getTwoFactor() {
        UtilEncryption.setKeyNumber(3);

        String key = UtilEncryption.encrypt("twoFactorSecret");

        UtilEncryption.setKeyNumber(2);

        if (infoConfig.getString(key) != null) {
            return UtilEncryption.decrypt(infoConfig.getString(key));
        }

        return null;
    }

    protected static void setTwoFactor(String twoFactor) {
        UtilEncryption.setKeyNumber(3);

        String key = UtilEncryption.encrypt("twoFactorSecret");

        UtilEncryption.setKeyNumber(2);

        infoConfig.set(key, UtilEncryption.encrypt(twoFactor));
    }
}
