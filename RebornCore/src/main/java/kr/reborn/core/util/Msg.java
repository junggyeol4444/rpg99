package kr.reborn.core.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class Msg {
    public static final String PREFIX = ChatColor.GOLD + "[환생] " + ChatColor.RESET;

    private Msg() {}

    public static String c(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static void send(CommandSender to, String msg) {
        to.sendMessage(PREFIX + c(msg));
    }

    public static void warn(CommandSender to, String msg) {
        to.sendMessage(PREFIX + ChatColor.YELLOW + c(msg));
    }

    public static void error(CommandSender to, String msg) {
        to.sendMessage(PREFIX + ChatColor.RED + c(msg));
    }
}
