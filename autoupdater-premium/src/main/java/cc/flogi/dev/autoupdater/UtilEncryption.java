package cc.flogi.dev.autoupdater;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 6/14/17
 */
final class UtilEncryption {
    private static SecretKeySpec secretKey;
    private static File keyFile;
    private static FileConfiguration keyConfig;

    protected static void init() {
        if (keyFile == null)
            keyFile = new File(AutoUpdaterInternal.getDataFolder().getAbsolutePath() + "/.keys.enc");

        if (!keyFile.getParentFile().exists())
            keyFile.getParentFile().mkdirs();

        if (keyConfig == null) {
            keyConfig = YamlConfiguration.loadConfiguration(keyFile);
            keyConfig.options().copyDefaults(true);
        }

        if (keyConfig.get("0") == null)
            keyConfig.set("0", RandomStringUtils.randomAscii(20));
        if (keyConfig.get("1") == null)
            keyConfig.set("1", RandomStringUtils.randomAscii(20));
        if (keyConfig.get("2") == null)
            keyConfig.set("2", RandomStringUtils.randomAscii(20));
        if (keyConfig.get("3") == null)
            keyConfig.set("3", RandomStringUtils.randomAscii(20));

        try {
            if (!keyFile.exists() && !keyFile.createNewFile())
                throw new IOException("Key file generation failed.");

            keyFile.setReadable(true, true);
            keyFile.setWritable(true, true);

            keyConfig.save(keyFile);
        } catch (IOException ex) {
            AutoUpdaterInternal.get().printError(ex, "Error occurred while creating the encrypted file.");
        }
    }

    protected static void setKeyNumber(int number) {
        setKey(keyConfig.getString(String.valueOf(number)));
    }

    private static void setKey(String myKey) {
        MessageDigest sha;
        try {
            byte[] key = myKey.getBytes(StandardCharsets.UTF_8);
            sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = java.util.Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
        } catch (NoSuchAlgorithmException ex) {
            AutoUpdaterInternal.get().printError(ex);
        }
    }

    protected static String encrypt(String strToEncrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

            cipher.init(1, secretKey);

            return Base64.encodeBase64String(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            AutoUpdaterInternal.get().printError(ex, "Error occurred while encrypting string.");
        }

        return null;
    }

    protected static String decrypt(String strToDecrypt) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");

            cipher.init(2, secretKey);
            return new String(cipher.doFinal(Base64.decodeBase64(strToDecrypt)));
        } catch (Exception ex) {
            AutoUpdaterInternal.get().printError(ex, "Error occurred while encrypting string.");
        }

        return null;
    }
}
