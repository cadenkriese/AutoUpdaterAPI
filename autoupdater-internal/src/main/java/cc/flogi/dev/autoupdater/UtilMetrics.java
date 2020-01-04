package cc.flogi.dev.autoupdater;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 1/1/20
 */
public final class UtilMetrics {
    private static final String BASE_URL = "https://api.flogi.cc/plugins";
    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    private static String token;
    private static LocalDateTime expirationDate;

    public static void sendUpdateInfo(Plugin plugin, PluginUpdate update) {
        if (InternalCore.METRICS) {
            if (dbContainsPlugin(plugin.name)) {
                plugin.updates = null;
                sendRequest("POST", BASE_URL + "/" + plugin.name + "/updates", GSON.toJson(update));
                sendRequest("PUT", BASE_URL + "/" + plugin.name + "/updates", GSON.toJson(plugin));
            } else {
                plugin.updates = Collections.singletonList(update);
                sendRequest("POST", BASE_URL + "?type=public", GSON.toJson(plugin));
            }
        }
    }

    private static boolean dbContainsPlugin(String pluginName) {
        try {
            UtilReader.readFrom(BASE_URL + "/" + pluginName);
            return true;
        } catch (IOException ex) {
            //Print weird exceptions, FileNotFound is expected on 404.
            if (InternalCore.DEBUG && !(ex instanceof FileNotFoundException))
                InternalCore.get().printError(ex);

            return false;
        }
    }

    private static void sendRequest(String requestMethod, String urlString, String body) {
        auth();

        StringBuilder requestData = new StringBuilder();

        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(requestMethod.toUpperCase());
            conn.setDoOutput(true);

            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Content-Length", Integer.toString(body.length()));

            conn.setFixedLengthStreamingMode(body.length());
            conn.connect();

            try (DataOutputStream writer = new DataOutputStream(conn.getOutputStream())) {
                writer.write(body.getBytes(StandardCharsets.UTF_8));
                writer.flush();
            } finally {
                conn.disconnect();
            }
        } catch (IOException ex) {
            if (InternalCore.DEBUG && !(ex instanceof FileNotFoundException))
                InternalCore.get().printError(ex);
        }
    }

    private static void auth() {
        if (token == null || LocalDateTime.now().isAfter(expirationDate)) {
            try {
                JsonParser parser = new JsonParser();
                JsonObject obj = parser.parse(UtilReader.readFrom(BASE_URL + "/auth")).getAsJsonObject();
                token = obj.get("token").getAsString();
                expirationDate = LocalDateTime.now().plusDays(7);
            } catch (IOException ex) {
                if (InternalCore.DEBUG)
                    InternalCore.get().printError(ex);
            }
        }
    }

    @AllArgsConstructor @Getter @Setter
    public static final class PluginUpdateVersion {
        @SerializedName("old")
        private String oldVersion;
        @SerializedName("new")
        private String newVersion;
    }

    @AllArgsConstructor @Getter @Setter
    public static final class PluginUpdate {
        PluginUpdate(Date timestamp, PluginUpdateVersion version) {
            this.timestamp = timestamp;
            this.version = version;
        }

        private Date timestamp;
        private Long size;
        @SerializedName("update_duration")
        private Double updateDuration;
        private PluginUpdateVersion version;
    }

    @AllArgsConstructor @Getter @Setter
    public static class Plugin {
        private Plugin() {}
        Plugin(String name, String description, String downloadUrl) {
            this(name, description, downloadUrl, null);
        }

        private String name;
        private String description;
        @SerializedName("download_url")
        private String downloadUrl;
        private List<PluginUpdate> updates;
    }

    @Getter @Setter
    public static final class SpigotPlugin extends Plugin {
        SpigotPlugin(String name, String description, String downloadUrl, String spigotName, int resourceId,
                     double averageRating, Date uploadDate, String[] supportedVersions) {
            this(name, description, downloadUrl, spigotName, resourceId, averageRating, uploadDate, supportedVersions, false, null, null);
        }

        SpigotPlugin(String name, String description, String downloadUrl, String spigotName, int resourceId,
                     double averageRating, Date uploadDate, String[] supportedVersions, boolean premium, Double price, String currency) {
            super(name, description, downloadUrl);

            this.spigotName = spigotName;
            this.resourceId = resourceId;
            this.averageRating = averageRating;
            this.uploadDate = uploadDate;
            this.supportedVersions = supportedVersions;
            this.premium = premium;
            this.price = price;
            this.currency = currency;
        }

        @SerializedName("spigot_name")
        private String spigotName;
        @SerializedName("resource_id")
        private int resourceId;
        private String category;
        @SerializedName("average_rating")
        private Double averageRating;
        @SerializedName("upload_date")
        private Date uploadDate;
        @SerializedName("supported_versions")
        private String[] supportedVersions;
        private Boolean premium;
        private Double price;
        private String currency;
    }
}
