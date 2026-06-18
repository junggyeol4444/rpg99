package kr.reborn.curse.command;

import kr.reborn.core.util.Msg;
import kr.reborn.curse.RebornCurse;
import kr.reborn.curse.data.ActiveEffect;
import kr.reborn.curse.data.EffectDef;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class CurseCommand implements CommandExecutor {
    private final RebornCurse plugin;
    public CurseCommand(RebornCurse plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String label, @NotNull String[] a) {
        if (a.length == 0) {
            Msg.send(s, "&7/curse apply <p> <id> | cure <p> <id> | list <p>");
            Msg.send(s, "&7         | berserk <p> | info <id> | catalog [BLESSING|CURSE] | mechanic <p> <id>");
            return true;
        }
        String sub = a[0].toLowerCase();
        // 모든 명령어가 관리자 권한 요구
        if (!s.hasPermission("reborncurse.admin") && !"list".equals(sub)
                && !"info".equals(sub) && !"catalog".equals(sub)) {
            Msg.error(s, "권한 없음"); return true;
        }
        switch (sub) {
            case "apply", "cure" -> {
                if (a.length < 3) { Msg.warn(s, "/curse " + sub + " <player> <id>"); return true; }
                Player tgt = Bukkit.getPlayer(a[1]);
                if (tgt == null) { Msg.error(s, "오프라인: " + a[1]); return true; }
                if ("apply".equals(sub)) plugin.effects().apply(tgt, a[2]);
                else plugin.effects().cure(tgt, a[2]);
            }
            case "list" -> {
                Player tgt;
                if (a.length >= 2) tgt = Bukkit.getPlayer(a[1]);
                else if (s instanceof Player p) tgt = p;
                else { Msg.warn(s, "/curse list <player>"); return true; }
                if (tgt == null) { Msg.error(s, "오프라인"); return true; }
                showList(s, tgt);
            }
            case "berserk" -> {
                if (a.length < 2) { Msg.warn(s, "/curse berserk <player>"); return true; }
                Player tgt = Bukkit.getPlayer(a[1]);
                if (tgt == null) { Msg.error(s, "오프라인"); return true; }
                // 강제 광폭화 (임시 ActiveEffect)
                EffectDef def = plugin.registry().get("qi_deviation");
                if (def != null) {
                    ActiveEffect a2 = plugin.effects().of(tgt.getUniqueId())
                            .computeIfAbsent("qi_deviation",
                                    k -> new ActiveEffect("qi_deviation", EffectDef.Kind.CURSE, 60, 1));
                    plugin.berserk().start(tgt, a2);
                    Msg.send(s, "&c강제 광폭화 발동: " + tgt.getName());
                }
            }
            case "info" -> {
                if (a.length < 2) { Msg.warn(s, "/curse info <id>"); return true; }
                EffectDef def = plugin.registry().get(a[1]);
                if (def == null) { Msg.error(s, "정의 없음: " + a[1]); return true; }
                showInfo(s, def);
            }
            case "catalog" -> {
                String filter = a.length >= 2 ? a[1].toUpperCase() : "";
                Msg.send(s, "&6=== 효과 카탈로그 ===");
                if (!"CURSE".equals(filter)) {
                    s.sendMessage("§b── 축복 (" + plugin.registry().blessings().size() + ") ──");
                    for (EffectDef d : plugin.registry().blessings().values()) {
                        s.sendMessage("§7• §e" + d.id + " §7- " + d.name);
                    }
                }
                if (!"BLESSING".equals(filter)) {
                    s.sendMessage("§c── 저주 (" + plugin.registry().curses().size() + ") ──");
                    for (EffectDef d : plugin.registry().curses().values()) {
                        s.sendMessage("§7• §c" + d.id + " §7- " + d.name);
                    }
                }
            }
            case "mechanic" -> {
                if (a.length < 3) { Msg.warn(s, "/curse mechanic <player> <id>"); return true; }
                Player tgt = Bukkit.getPlayer(a[1]);
                if (tgt == null) { Msg.error(s, "오프라인"); return true; }
                if (plugin.cure().tryCureMechanic(tgt, a[2])) Msg.send(s, "&a해제 시도 완료.");
                else Msg.warn(s, "해당 매커니즘으로 해제되는 저주 없음.");
            }
            default -> Msg.warn(s, "알 수 없는 하위 명령: " + sub);
        }
        return true;
    }

    private void showList(CommandSender s, Player tgt) {
        var map = plugin.effects().of(tgt.getUniqueId());
        Msg.send(s, "&6=== " + tgt.getName() + " 효과 (" + map.size() + ") ===");
        if (map.isEmpty()) { s.sendMessage("§7— 활성 효과 없음."); return; }
        for (ActiveEffect a : map.values()) {
            EffectDef def = plugin.registry().get(a.id);
            if (def == null) continue;
            String dur = a.isPermanent() ? "§7영구" : "§f" + a.remainingTicks + "초";
            String b = a.berserkActive ? " §c[광폭]" : "";
            s.sendMessage("§7• " + def.name + " §8(" + a.id + ") " + dur
                    + " §7stk:§f" + a.stacks + b);
        }
        // 광폭화 잔여
        long brem = plugin.berserk().remainingMs(tgt.getUniqueId());
        if (brem > 0) s.sendMessage("§c광폭화 종료까지: " + (brem / 1000) + "초");
        // 봉인 스쿨
        var locked = plugin.special().lockedSchoolsView().get(tgt.getUniqueId());
        if (locked != null && !locked.isEmpty()) {
            s.sendMessage("§c봉인된 스킬 스쿨: §f" + String.join(", ", locked));
        }
        // 수련 효율
        double eff = plugin.special().trainEfficiencyModifier(tgt.getUniqueId());
        if (eff != 0) s.sendMessage("§e수련 효율 모디파이어: §f" + (int) (eff * 100) + "%");
    }

    private void showInfo(CommandSender s, EffectDef def) {
        Msg.send(s, "&6=== " + def.name + " ===");
        s.sendMessage("§7유형: §f" + def.kind + " §7월드: §f"
                + (def.worldRestriction == null ? "ANY" : def.worldRestriction));
        s.sendMessage("§7설명: §f" + def.description);
        s.sendMessage("§7지속: §f" + (def.permanent() ? "영구" : def.durationSeconds + "초"));
        s.sendMessage("§7최대 중첩: §f" + def.maxStacks + " §7틱 간격: §f"
                + def.tickIntervalSeconds + "s");
        if (!def.staticStats.isEmpty()) s.sendMessage("§7즉시 보정: §e" + def.staticStats);
        if (!def.tickStats.isEmpty()) s.sendMessage("§7틱 보정: §6" + def.tickStats);
        if (!def.tickStatsDayOnly.isEmpty())
            s.sendMessage("§7낮 한정 틱: §6" + def.tickStatsDayOnly);
        if (!def.percentStats.isEmpty())
            s.sendMessage("§7비율 보정: §c" + def.percentStats);
        if (def.tickStatsCommon != 0)
            s.sendMessage("§7공통 8 틱: §c" + def.tickStatsCommon);
        if (def.berserkChance > 0)
            s.sendMessage("§c광폭화 확률: §f" + (def.berserkChance * 100) + "%");
        if (def.hpTick != 0) s.sendMessage("§4HP 틱: §f" + def.hpTick);
        if (def.outOfShipHpDrain != 0)
            s.sendMessage("§3육지 이탈 HP 드레인: §f" + def.outOfShipHpDrain);
        if (def.lockSkillSchool != null)
            s.sendMessage("§5봉인 스쿨: §f" + def.lockSkillSchool);
        if (def.trainEfficiency != 0)
            s.sendMessage("§e수련 효율: §f" + (int) (def.trainEfficiency * 100) + "%");
        if (def.npcFavorTick != 0)
            s.sendMessage("§dNPC 호의도 틱: §f" + def.npcFavorTick);
        if (def.special != null) s.sendMessage("§d특수: §f" + def.special);
        if (!def.cureMethods.isEmpty())
            s.sendMessage("§a해제 방법: §f" + String.join(", ", def.cureMethods));
    }
}
