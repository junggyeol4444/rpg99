package kr.reborn.npc.famous;

import kr.reborn.core.util.Msg;
import kr.reborn.npc.RebornNPC;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 유명 NPC 첫 만남 이벤트.
 *
 * 플레이어가 처음 유명 NPC를 만나면 특별한 연출:
 *   - 화면 전체 깜빡임 (chat clear + 헤더)
 *   - 권력 등급 색 시각 효과
 *   - "처음 ___ 를 만났다" 칭호 부여 (RebornTitle)
 *   - 광역 broadcast (LEGEND 인물만)
 *   - NPC 설명 표시
 */
public final class FamousEncounter {

    private static final String NS = "RebornNPC.famousMet";

    private final RebornNPC plugin;
    /** uuid → 만난 famous NPC id set */
    private final Map<UUID, java.util.Set<String>> firstMet = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> loaded = ConcurrentHashMap.newKeySet();

    public FamousEncounter(RebornNPC plugin) { this.plugin = plugin; }

    private void ensureLoaded(UUID p) {
        if (loaded.add(p)) {
            String csv = kr.reborn.core.RebornCore.get().kv().get(NS, p, "met");
            if (csv != null && !csv.isEmpty()) {
                java.util.Set<String> set = new java.util.HashSet<>();
                for (String c : csv.split(",")) if (!c.isEmpty()) set.add(c);
                firstMet.put(p, set);
            }
        }
    }

    /** NpcInteractListener에서 호출 — 첫 만남이면 특별 연출. */
    public boolean tryFirstEncounter(Player p, String npcId) {
        FamousNpc fn = plugin.famous().get(npcId);
        if (fn == null) return false;
        ensureLoaded(p.getUniqueId());
        var set = firstMet.computeIfAbsent(p.getUniqueId(), k -> new java.util.HashSet<>());
        if (set.contains(npcId)) return false;
        set.add(npcId);
        kr.reborn.core.RebornCore.get().kv().put(NS, p.getUniqueId(), "met", String.join(",", set));
        renderEncounter(p, fn);
        return true;
    }

    private void renderEncounter(Player p, FamousNpc fn) {
        // 화면 클리어 + 헤더
        for (int i = 0; i < 5; i++) p.sendMessage("");
        Msg.send(p, "&8&l═════════════════════════════════");
        Msg.send(p, "");
        Msg.send(p, "&6&l    ✦ 운명의 만남 ✦");
        Msg.send(p, "");
        Msg.send(p, "    " + fn.displayName);
        Msg.send(p, "");
        Msg.send(p, "&7    " + fn.title + " §8(권력 " + fn.powerRank + "/10)");
        Msg.send(p, "");
        p.sendMessage("§7    " + fn.description);
        Msg.send(p, "");
        Msg.send(p, "&8&l═════════════════════════════════");

        // 권력별 시각 효과
        if (fn.powerRank >= 10) {
            // 절대자급
            p.sendTitle("§6§l✦ " + cleanName(fn.displayName) + " ✦",
                    "§e" + fn.title, 20, 80, 20);
            try {
                p.getWorld().spawnParticle(Particle.TOTEM, p.getLocation().add(0, 1, 0), 100, 1.5, 2, 1.5);
                p.getWorld().playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BELL_RESONATE, 1.5f, 0.5f);
            } catch (Throwable ignored) {}
            Bukkit.broadcastMessage("§6§l[운명의 만남] §f"
                    + p.getName() + " §7가 §6" + cleanName(fn.displayName)
                    + " §7과(와) 처음 마주쳤다.");
        } else if (fn.powerRank >= 8) {
            p.sendTitle("§5§l✦ " + cleanName(fn.displayName) + " ✦",
                    "§d" + fn.title, 10, 60, 20);
            try {
                p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().add(0, 1, 0), 60, 1, 2, 1);
                p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);
            } catch (Throwable ignored) {}
        } else {
            p.sendTitle("§e" + cleanName(fn.displayName), "§7" + fn.title, 10, 40, 10);
            try {
                p.getWorld().spawnParticle(Particle.HEART, p.getLocation().add(0, 1, 0), 20, 0.5, 1, 0.5);
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            } catch (Throwable ignored) {}
        }

        // 업적 자동 진척
        try {
            var tp = Bukkit.getPluginManager().getPlugin("RebornTitle");
            if (tp != null) {
                Object am = tp.getClass().getMethod("achievements").invoke(tp);
                am.getClass().getMethod("incrementProgress",
                                Player.class, String.class, int.class)
                        .invoke(am, p, "famous_met", 1);
            }
        } catch (Throwable ignored) {}
    }

    private String cleanName(String s) {
        if (s == null) return "?";
        return s.replaceAll("[§&][0-9a-fk-or]", "");
    }

    public java.util.Set<String> metOf(UUID p) {
        ensureLoaded(p);
        return firstMet.getOrDefault(p, java.util.Collections.emptySet());
    }

    public int metCount(UUID p) {
        ensureLoaded(p);
        return firstMet.getOrDefault(p, java.util.Collections.emptySet()).size();
    }
}
