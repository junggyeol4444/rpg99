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

    /** 다국어 — Lang.yml 키로 메시지 전송. {0}, {1} 치환. */
    public static void t(CommandSender to, String key, Object... args) {
        to.sendMessage(PREFIX + c(Lang.t(to, key, args)));
    }

    public static void tWarn(CommandSender to, String key, Object... args) {
        to.sendMessage(PREFIX + ChatColor.YELLOW + c(Lang.t(to, key, args)));
    }

    public static void tError(CommandSender to, String key, Object... args) {
        to.sendMessage(PREFIX + ChatColor.RED + c(Lang.t(to, key, args)));
    }
}
