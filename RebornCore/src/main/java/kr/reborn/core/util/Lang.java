package kr.reborn.core.util;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 다국어 메시지 룩업.
 *
 * 사용:
 *   Msg.t(sender, "common.no-permission")
 *   Msg.t(sender, "stat.up", statName, amount)   // {0}, {1} 치환
 *
 * 키가 없으면 키 자체를 반환 (개발자에게 누락 노출).
 *
 * 언어 결정 순서:
 *   1. 플레이어가 /lang ko|en 으로 설정한 KV 값 (RebornCore.lang.player NS)
 *   2. RebornCore config.yml의 lang.default
 *   3. "ko"
 *
 * 콘솔/명령블록은 항상 서버 기본 언어.
 */
public final class Lang {

    private static final String NS = "RebornCore.lang.player";

    private static final Map<String, Map<String, String>> BUNDLES = new HashMap<>();
    private static final Map<UUID, String> playerOverride = new ConcurrentHashMap<>();
    private static String defaultLang = "ko";

    private Lang() {}

    /** RebornCore.onEnable 에서 1회 호출. */
    public static void init(JavaPlugin core) {
        defaultLang = core.getConfig().getString("lang.default", "ko").toLowerCase();
        loadBundle(core, "ko");
        loadBundle(core, "en");
    }

    private static void loadBundle(JavaPlugin core, String code) {
        Map<String, String> map = new HashMap<>();
        try (InputStream in = core.getResource("lang/" + code + ".yml")) {
            if (in != null) {
                YamlConfiguration yml = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(in, StandardCharsets.UTF_8));
                for (String key : yml.getKeys(true)) {
                    if (yml.isString(key)) map.put(key, yml.getString(key));
                }
            }
        } catch (Throwable t) {
            core.getLogger().warning("lang/" + code + ".yml 로드 실패: " + t.getMessage());
        }
        BUNDLES.put(code, map);
    }

    public static String resolveLang(CommandSender sender) {
        if (sender instanceof Player p) {
            String over = playerOverride.get(p.getUniqueId());
            if (over != null) return over;
            try {
                String stored = kr.reborn.core.RebornCore.get().kv().get(NS, p.getUniqueId(), "code");
                if (stored != null && !stored.isEmpty() && BUNDLES.containsKey(stored)) {
                    playerOverride.put(p.getUniqueId(), stored);
                    return stored;
                }
            } catch (Throwable ignored) {}
        }
        return defaultLang;
    }

    public static String t(CommandSender sender, String key, Object... args) {
        return format(BUNDLES.getOrDefault(resolveLang(sender), Map.of()), key, args);
    }

    public static String tLang(String langCode, String key, Object... args) {
        return format(BUNDLES.getOrDefault(langCode, Map.of()), key, args);
    }

    private static String format(Map<String, String> bundle, String key, Object... args) {
        String raw = bundle.get(key);
        if (raw == null) {
            // ko 폴백
            raw = BUNDLES.getOrDefault("ko", Map.of()).get(key);
        }
        if (raw == null) return key;
        if (args.length == 0) return raw;
        StringBuilder sb = new StringBuilder(raw.length() + 16);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '{' && i + 2 < raw.length() && raw.charAt(i + 2) == '}') {
                int idx = raw.charAt(i + 1) - '0';
                if (idx >= 0 && idx < args.length) {
                    sb.append(String.valueOf(args[idx]));
                    i += 2;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /** /lang 명령에서 호출. 빈 문자열이면 override 제거 = 서버 기본. */
    public static boolean setPlayerLang(Player p, String code) {
        code = code == null ? "" : code.toLowerCase();
        if (code.isEmpty()) {
            playerOverride.remove(p.getUniqueId());
            try { kr.reborn.core.RebornCore.get().kv().put(NS, p.getUniqueId(), "code", ""); }
            catch (Throwable ignored) {}
            return true;
        }
        if (!BUNDLES.containsKey(code)) return false;
        playerOverride.put(p.getUniqueId(), code);
        try { kr.reborn.core.RebornCore.get().kv().put(NS, p.getUniqueId(), "code", code); }
        catch (Throwable ignored) {}
        return true;
    }

    public static void setDefaultLang(String code) {
        if (BUNDLES.containsKey(code)) defaultLang = code;
    }

    public static String defaultLang() { return defaultLang; }
    public static java.util.Set<String> available() { return BUNDLES.keySet(); }
}
