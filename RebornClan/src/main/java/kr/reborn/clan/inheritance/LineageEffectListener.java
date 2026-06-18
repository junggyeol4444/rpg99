package kr.reborn.clan.inheritance;

import kr.reborn.clan.RebornClan;
import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.util.Msg;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 혈통 특수 효과 (기획서 23장).
 *
 * 플레이어의 PlayerData.lineage() 값에 따라 자동으로 영구 보너스 부여.
 * 보너스는 source="lineage:<TYPE>"으로 기록되어 중복 적용 차단.
 *
 * 지원 혈통:
 *   ROYAL — 카리스마 +30 (왕족)
 *   ELF — 마나 +200, 매력 +20 (엘프)
 *   DEMON_LORD — 마기 +500, 마교 비급 친화도 (마왕 자손)
 *   DEMON_NOBLE — 마기 +200, 카리스마 +15 (악마 귀족)
 *   KITSUNE — 요기 +300, 매력 +40 (구미호 혈통)
 *   GOLD_DRAGON — 용력 +50, 드래곤 브레스 자동 학습 (드래곤 혈통)
 *   SPIRIT — 정령력 +200, 정령 친화 (정령 혈통)
 *   MARTIAL_PURE — 내공 +300, 깨달음 +10 (무가 명문 후손)
 */
public final class LineageEffectListener implements Listener {

    private static final String NS = "RebornClan.lineageApplied";

    private final RebornClan plugin;
    /** 이번 세션에 적용된 보너스 lineage 추적 — 중복 방지. */
    private final Map<UUID, String> appliedInSession = new ConcurrentHashMap<>();

    public LineageEffectListener(RebornClan plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (d == null) return;
        String lineage = d.lineage();
        if (lineage == null || lineage.isEmpty()) return;
        // KV에 기록된 적용 여부 — 한 번만 적용
        String applied = RebornCore.get().kv().get(NS, p.getUniqueId(), "lineage");
        if (lineage.equals(applied)) {
            appliedInSession.put(p.getUniqueId(), lineage);
            return;
        }
        applyLineage(p, lineage);
        RebornCore.get().kv().put(NS, p.getUniqueId(), "lineage", lineage);
        appliedInSession.put(p.getUniqueId(), lineage);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        appliedInSession.remove(e.getPlayer().getUniqueId());
    }

    private void applyLineage(Player p, String lineage) {
        UUID id = p.getUniqueId();
        String src = "lineage:" + lineage;
        boolean handled = true;
        switch (lineage) {
            case "ROYAL" -> {
                RebornCore.get().api().addStat(id, StatType.CHARISMA, 30, src);
                Msg.send(p, "&6&l[혈통] §f왕족의 혈통이 깨어났다 — 카리스마 +30");
            }
            case "ELF" -> {
                RebornCore.get().api().addStat(id, StatType.MANA, 200, src);
                RebornCore.get().api().addStat(id, StatType.CHARM, 20, src);
                Msg.send(p, "&a&l[혈통] §f엘프의 혈통 — 마나 +200, 매력 +20");
            }
            case "DEMON_LORD" -> {
                RebornCore.get().api().addStat(id, StatType.DEMON_KI, 500, src);
                RebornCore.get().api().addStat(id, StatType.STRENGTH, 20, src);
                Msg.send(p, "&5&l[혈통] §f마왕의 혈통이 깨어났다 — 마기 +500, 근력 +20");
            }
            case "DEMON_NOBLE" -> {
                RebornCore.get().api().addStat(id, StatType.DEMON_KI, 200, src);
                RebornCore.get().api().addStat(id, StatType.CHARISMA, 15, src);
                Msg.send(p, "&5[혈통] §f악마 귀족의 혈통 — 마기 +200, 카리스마 +15");
            }
            case "KITSUNE" -> {
                RebornCore.get().api().addStat(id, StatType.YOKAI_KI, 300, src);
                RebornCore.get().api().addStat(id, StatType.CHARM, 40, src);
                Msg.send(p, "&d&l[혈통] §f구미호 혈통 — 요기 +300, 매력 +40");
            }
            case "GOLD_DRAGON" -> {
                RebornCore.get().api().addStat(id, StatType.DRAGON_POWER, 50, src);
                Msg.send(p, "&6&l[혈통] §f드래곤의 혈통 — 용력 +50, 브레스 사용 가능");
                // 드래곤 브레스 자동 학습 (RebornSkill 리플렉션)
                try {
                    var sp = org.bukkit.Bukkit.getPluginManager().getPlugin("RebornSkill");
                    if (sp != null) {
                        sp.getClass().getMethod("learnByApi", UUID.class, String.class)
                                .invoke(sp, id, "breath_basic");
                    }
                } catch (Throwable ignored) {}
            }
            case "SPIRIT" -> {
                RebornCore.get().api().addStat(id, StatType.SPIRIT_POWER, 200, src);
                Msg.send(p, "&b&l[혈통] §f정령의 혈통 — 정령력 +200");
            }
            case "MARTIAL_PURE" -> {
                RebornCore.get().api().addStat(id, StatType.INNER_KI, 300, src);
                RebornCore.get().api().addStat(id, StatType.MENTAL, 10, src);
                Msg.send(p, "&3&l[혈통] §f무가 명문의 혈통 — 내공 +300, 정신 +10");
            }
            default -> handled = false;
        }
        if (handled) {
            org.bukkit.Bukkit.broadcastMessage("§7§o[" + p.getName() + "의 혈통이 깨어났다: §f"
                    + lineage + "§7]");
        }
    }
}
