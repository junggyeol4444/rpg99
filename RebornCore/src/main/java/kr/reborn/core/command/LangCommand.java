package kr.reborn.core.command;

import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Lang;
import kr.reborn.core.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /lang [code|default|server <code>]
 *   /lang            — 현재 언어 + 사용 가능 목록.
 *   /lang ko|en      — 자기 언어 override.
 *   /lang default    — 자기 override 제거 (서버 기본 사용).
 *   /lang server ko  — 서버 기본 언어 변경 (admin).
 */
public final class LangCommand implements CommandExecutor {

    private final RebornCore plugin;
    public LangCommand(RebornCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (a.length == 0) {
            Msg.t(s, "lang.current", Lang.resolveLang(s));
            Msg.t(s, "lang.available", String.join(", ", Lang.available()));
            return true;
        }
        String sub = a[0].toLowerCase();
        if ("server".equals(sub)) {
            if (!s.hasPermission("reborncore.admin") && !s.isOp()) {
                Msg.t(s, "common.no-permission");
                return true;
            }
            if (a.length < 2) { Msg.tWarn(s, "common.unknown-arg", "/lang server <code>"); return true; }
            String code = a[1].toLowerCase();
            if (!Lang.available().contains(code)) { Msg.tError(s, "lang.invalid", code); return true; }
            Lang.setDefaultLang(code);
            plugin.getConfig().set("lang.default", code);
            plugin.saveConfig();
            Msg.t(s, "lang.set-default", code);
            return true;
        }
        if (!(s instanceof Player p)) { Msg.t(s, "common.player-only"); return true; }
        if ("default".equals(sub)) {
            Lang.setPlayerLang(p, "");
            Msg.t(p, "lang.set", Lang.defaultLang());
            return true;
        }
        if (!Lang.setPlayerLang(p, sub)) {
            Msg.tError(p, "lang.invalid", sub);
            return true;
        }
        Msg.t(p, "lang.set", sub);
        return true;
    }
}
