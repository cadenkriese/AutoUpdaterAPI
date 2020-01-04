package cc.flogi.dev.autoupdater;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;

/**
 * @author Caden Kriese (flogic)
 *
 * Created on 2/27/18
 */
public final class UtilUI {
    private static final long ACTIONBAR_DEFAULT_DURATION = 40;
    private static HashMap<String, BukkitRunnable> currentTasks = new HashMap<>();

    /**
     * Sends an action bar message to the player.
     *
     * @param player  The player to receive the message.
     * @param message The message to be sent (with color codes to be replaced).
     */
    public static void sendActionBar(Player player, String message) {
        if (player == null)
            return;

        clearActionBar(player);

        if (!Bukkit.isPrimaryThread()) {
            UtilThreading.sync(() -> actionBar(player, message));
        }

        actionBar(player, message);
    }

    /**
     * Sends an action bar message to the player.
     * The duration of the message has a minimum of about 60 ticks, due to how minecraft sends actionbars.
     *
     * @param player   The player to send the message to.
     * @param message  The message to be sent.
     * @param duration The duration of the message in seconds.
     */
    public static void sendActionBar(Player player, String message, long duration) {
        if (player == null)
            return;

        clearActionBar(player);
        final String uuid = player.getUniqueId().toString();

        BukkitRunnable actionBarRunnable = new BukkitRunnable() {
            long displayedDuration = 0;
            long totalDuration = duration * 20;

            @Override public void run() {
                if (player != null && player.isOnline() && displayedDuration < totalDuration) {
                    actionBar(player, message);
                    displayedDuration += ACTIONBAR_DEFAULT_DURATION;
                } else {
                    currentTasks.remove(uuid);
                    cancel();
                }
            }
        };

        currentTasks.put(player.getUniqueId().toString(), actionBarRunnable);
        actionBarRunnable.runTaskTimer(AutoUpdaterInternal.getPlugin(), 0L, ACTIONBAR_DEFAULT_DURATION);
    }

    /**
     * Clears the actionbar of a certain player.
     *
     * @param player The player to clear the actionbar of.
     */
    public static void clearActionBar(Player player) {
        if (player == null)
            return;

        if (currentTasks.containsKey(player.getUniqueId().toString())) {
            currentTasks.get(player.getUniqueId().toString()).cancel();
            currentTasks.remove(player.getUniqueId().toString());
        }

        actionBar(player, "");
    }

    /**
     * Sends a player a title.
     *
     * @param player  The player to receive the title.
     * @param title   The text to be displayed. (Color codes supported)
     * @param fadeIn  The fade in duration in ticks.
     * @param stay    The stay duration in ticks.
     * @param fadeOut The fade out duration in ticks.
     */
    public static void sendTitle(Player player, String title, int fadeIn, int stay, int fadeOut) {
        sendTitle(player, title, "", 20, 40, 20);
    }

    /**
     * Sends a player a title and subtitle.
     *
     * @param player   The player to receive the title.
     * @param title    The text to be displayed. (Color codes supported)
     * @param subtitle The subtitle text to be displayed. (Color codes supported)
     * @param fadeIn   The fade in duration in ticks.
     * @param stay     The stay duration in ticks.
     * @param fadeOut  The fade out duration in ticks.
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null)
            return;

        player.sendTitle(colorize(title), colorize(subtitle), fadeIn, stay, fadeOut);
    }

    /**
     * Generates a progress bar with ChatColors.
     *
     * @param barSize     The length of the progress bar.
     * @param numerator   The numerator of the fraction representing progress on the bar.
     * @param denominator The denominator of the fraction representing progress on the bar.
     * @param barChar     The character the progress bar is made out of.
     * @param used        The color representing the used section of the bar.
     * @param free        The color representing the free section of the bar.
     * @return The generated progress bar.
     */
    public static String progressBar(int barSize, double numerator, double denominator, char barChar, ChatColor used, ChatColor free) {
        String bar = repeat(barChar, barSize);
        int usedAmount = (int) (numerator / denominator * (double) barSize);
        bar = used + bar.substring(0, usedAmount) + free + bar.substring(usedAmount);
        return bar;
    }

    /**
     * Formats a string with the variables in order.
     *
     * @param toFormat  The string to replace variables in.
     * @param variables The variables to replace in order of "key", "value", "key", "value", etc.
     * @return The formatted string with the variables replaced.
     */
    public static String format(String toFormat, String... variables) {
        for (int i = 0; i < variables.length; i += 2) {
            String variable = "%" + variables[i] + "%";
            String replacement = variables[i + 1];

            toFormat = toFormat.replace(variable, replacement);
        }
        return toFormat;
    }

    /**
     * Shorthand notation for ${@link ChatColor#translateAlternateColorCodes(char, String)}.
     *
     * @param string The string to be colorized.
     * @return The colorized string.
     */
    public static String colorize(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    private static String repeat(final char ch, final int repeat) {
        if (repeat <= 0) {
            return "";
        }
        final char[] buf = new char[repeat];
        for (int i = repeat - 1; i >= 0; i--) {
            buf[i] = ch;
        }
        return new String(buf);
    }

    private static void actionBar(Player player, String message) {
        message = colorize(message);
        if (Bukkit.getVersion().contains("Paper")) {
            player.sendActionBar(message);
        } else {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        }
    }
}
