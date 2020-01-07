package cc.flogi.dev.autoupdater.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
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
 * Created on 01/01/2020
 */
public final class UtilMetrics {
    private static final String API_BASE_URL = "https://api.flogi.cc/updater-metrics/v1";
    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    private static String token;
    private static LocalDateTime expirationDate;

    public static void sendUpdateInfo(Plugin plugin, PluginUpdate update) {
        if (AutoUpdaterInternal.METRICS) {
            String type = plugin instanceof SpigotPlugin ? "spigot" : "public";
            boolean dbContainsPlugin = dbContainsPlugin(plugin.getName());
            if (dbContainsPlugin) {
                plugin.setUpdates(null);
                sendRequest("POST", API_BASE_URL + "/plugins/" + plugin.name + "/updates", GSON.toJson(update));
                sendRequest("PUT", API_BASE_URL + "/plugins/" + plugin.name, GSON.toJson(plugin));
            } else {
                plugin.setUpdates(Collections.singletonList(update));
                sendRequest("POST", API_BASE_URL + "/plugins?type=" + type, GSON.toJson(plugin));
            }
        }
    }

    private static boolean dbContainsPlugin(String pluginName) {
        try {
            readFrom(API_BASE_URL + "/plugins/" + pluginName);
            return true;
        } catch (IOException ex) {
            //Print weird exceptions, FileNotFound is expected on 404.
            if (AutoUpdaterInternal.DEBUG && !(ex instanceof FileNotFoundException))
                ex.printStackTrace();

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
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Content-Length", Integer.toString(body.length()));

            conn.setFixedLengthStreamingMode(body.length());
            conn.connect();

            try (DataOutputStream writer = new DataOutputStream(conn.getOutputStream())) {
                writer.write(body.getBytes(StandardCharsets.UTF_8));
                writer.flush();

                int responseCode = conn.getResponseCode();

                if (responseCode == 401) {
                    try (InputStream is = conn.getInputStream()) {
                        String responseBody = buildStringFromInputStream(is);

                        if (responseBody.contains("token has expired")) {
                            token = null;
                            sendRequest(requestMethod, urlString, body);
                        }
                    }
                } else if (responseCode != 200 && AutoUpdaterInternal.DEBUG)
                    System.out.println("Metrics request failed code = " + responseCode);
            } finally {
                conn.disconnect();
            }
        } catch (IOException ex) {
            if (AutoUpdaterInternal.DEBUG && !(ex instanceof FileNotFoundException))
                ex.printStackTrace();
        }
    }

    private static void auth() {
        if (token == null || LocalDateTime.now().isAfter(expirationDate)) {
            try {
                JsonParser parser = new JsonParser();
                JsonObject obj = parser.parse(readFrom(API_BASE_URL + "/auth")).getAsJsonObject();
                token = obj.get("token").getAsString();
                expirationDate = LocalDateTime.now().plusDays(7);
            } catch (IOException ex) {
                if (AutoUpdaterInternal.DEBUG)
                    ex.printStackTrace();
            }
        }
    }

    /**
     * Reads content from a URL.
     *
     * Note has to be a duplicate of ${@link UtilReader#readFrom(String)} to maintain package-private scope.
     *
     * @param url The URL to read from.
     * @return The content from the page.
     * @throws IOException from stream.
     */
    static String readFrom(String url) throws IOException {
        try (InputStream is = new URL(url).openStream()) {
            return buildStringFromInputStream(is);
        }
    }

    private static String buildStringFromInputStream(InputStream inputStream) throws IOException {
        BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();

        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    @AllArgsConstructor @Getter @Setter
    static final class PluginUpdateVersion {
        @SerializedName("old")
        private String oldVersion;
        @SerializedName("new")
        private String newVersion;
    }

    @AllArgsConstructor @Getter @Setter
    static final class PluginUpdate {
        private Date timestamp;
        private Long size;
        @SerializedName("update_duration")
        private Double updateDuration;
        private PluginUpdateVersion version;

        PluginUpdate(Date timestamp, PluginUpdateVersion version) {
            this.timestamp = timestamp;
            this.version = version;
        }
    }

    @AllArgsConstructor @Getter @Setter
    static class Plugin {
        private String name;
        private String description;
        @SerializedName("download_url")
        private String downloadUrl;
        private List<PluginUpdate> updates;

        private Plugin() {}

        Plugin(String name, String description, String downloadUrl) {
            this(name, description, downloadUrl, null);
        }
    }

    @Getter @Setter
    static final class SpigotPlugin extends Plugin {
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

        SpigotPlugin(String name, String description, String downloadUrl, String spigotName, int resourceId,
                     String category, double averageRating, Date uploadDate, String[] supportedVersions) {
            this(name, description, downloadUrl, spigotName, resourceId, category, averageRating, uploadDate,
                    supportedVersions, false, null, null);
        }

        SpigotPlugin(String name, String description, String downloadUrl, String spigotName, int resourceId,
                     String category, double averageRating, Date uploadDate, String[] supportedVersions, boolean premium, Double price, String currency) {
            super(name, description, downloadUrl);

            this.spigotName = spigotName;
            this.resourceId = resourceId;
            this.category = category;
            this.averageRating = averageRating;
            this.uploadDate = uploadDate;
            this.supportedVersions = supportedVersions;
            this.premium = premium;
            this.price = price;
            this.currency = currency;
        }
    }
}
