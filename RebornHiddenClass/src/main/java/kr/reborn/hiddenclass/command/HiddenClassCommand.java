package kr.reborn.hiddenclass.command;

import kr.reborn.core.util.Msg;
import kr.reborn.hiddenclass.RebornHiddenClass;
import kr.reborn.hiddenclass.ability.HiddenAbility;
import kr.reborn.hiddenclass.data.HiddenClass;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class HiddenClassCommand implements CommandExecutor {

    private final RebornHiddenClass plugin;

    public HiddenClassCommand(RebornHiddenClass plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String label, @NotNull String[] a) {
        if (a.length == 0) {
            if (!(s instanceof Player p)) {
                Msg.send(s, "&7/hc list | info <id> | cast <ability> [target] | abilities | grant <player> <id>");
                return true;
            }
            showMine(p);
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "list" -> listAll(s);
            case "mine" -> { if (s instanceof Player p) showMine(p); }
            case "info" -> {
                if (a.length < 2) { Msg.send(s, "&7/hc info <id>"); return true; }
                showInfo(s, a[1]);
            }
            case "abilities", "ab" -> {
                if (!(s instanceof Player p)) return true;
                showAbilities(p);
            }
            case "cast" -> {
                if (!(s instanceof Player p)) return true;
                if (a.length < 2) { Msg.send(s, "&7/hc cast <ability> [target]"); return true; }
                HiddenAbility ab;
                try { ab = HiddenAbility.valueOf(a[1].toUpperCase()); }
                catch (IllegalArgumentException e) { Msg.error(p, "능력 없음: " + a[1]); return true; }
                String target = a.length >= 3 ? a[2] : "";
                plugin.abilities().cast(p, ab, target);
            }
            case "cooldown", "cd" -> {
                if (!(s instanceof Player p)) return true;
                showCooldowns(p);
            }
            case "grant" -> {
                if (!s.hasPermission("rebornhiddenclass.admin")) { Msg.error(s, "권한 없음."); return true; }
                if (a.length < 3) { Msg.send(s, "&7/hc grant <player> <id>"); return true; }
                Player tgt = Bukkit.getPlayerExact(a[1]);
                if (tgt == null) { Msg.error(s, "플레이어 없음."); return true; }
                if (plugin.engine().adminGrant(tgt, a[2])) Msg.send(s, "&a부여 완료: " + a[2]);
                else Msg.error(s, "부여 실패 — 이미 보유 or 클래스 없음.");
            }
            case "check" -> {
                if (!(s instanceof Player p)) return true;
                plugin.engine().fullCheck(p);
                Msg.send(p, "&a조건 풀체크 실행.");
            }
            default -> Msg.send(s, "&7/hc list | mine | info <id> | abilities | cast <a> [t] | cooldown | grant <p> <id> | check");
        }
        return true;
    }

    private void listAll(CommandSender s) {
        Msg.send(s, "&6=== 히든 클래스 전체 (" + plugin.registry().all().size() + ") ===");
        int shown = 0;
        for (HiddenClass hc : plugin.registry().all()) {
            if (shown++ >= 30) { s.sendMessage("§7… 추가 " + (plugin.registry().all().size() - shown) + "개"); break; }
            s.sendMessage("§7• §e" + hc.id + " §7- " + hc.name + " §8(" + hc.type + ")");
        }
    }

    private void showMine(Player p) {
        var unlocked = plugin.progress().unlocked(p.getUniqueId());
        Msg.send(p, "&6=== 내 히든 클래스 (" + unlocked.size() + ") ===");
        if (unlocked.isEmpty()) { p.sendMessage("§7— 없음. 조건을 충족하면 자동 해금됩니다."); return; }
        for (String id : unlocked) {
            HiddenClass hc = plugin.registry().get(id);
            if (hc == null) continue;
            p.sendMessage("§a✦ §f" + hc.name + " §7(" + hc.id + ")");
            p.sendMessage("  §8" + hc.description);
        }
    }

    private void showInfo(CommandSender s, String id) {
        HiddenClass hc = plugin.registry().get(id);
        if (hc == null) { Msg.error(s, "클래스 없음: " + id); return; }
        Msg.send(s, "&6=== " + hc.name + " ===");
        s.sendMessage("§7유형: §f" + hc.type + " §7월드: §f" + (hc.worldRestriction == null ? "ANY" : hc.worldRestriction));
        s.sendMessage("§7설명: §f" + hc.description);
        if (hc.type == HiddenClass.Type.INITIAL) {
            s.sendMessage("§7환생시 확률: §6" + (hc.initialChance * 100) + "%");
        }
        if (!hc.statBonuses.isEmpty()) {
            s.sendMessage("§7스탯 보너스: §e" + hc.statBonuses);
        }
        if (!hc.statOverrides.isEmpty()) {
            s.sendMessage("§7스탯 오버라이드: §c" + hc.statOverrides);
        }
        if (!hc.skills.isEmpty()) s.sendMessage("§7스킬 자동학습: §b" + hc.skills);
        if (hc.passive != null) s.sendMessage("§7패시브 키: §d" + hc.passive);
        if (!hc.conditions.isEmpty()) {
            s.sendMessage("§7조건 (AND):");
            for (var c : hc.conditions) {
                s.sendMessage("  §8- " + c.type + " " + (c.stat != null ? c.stat : "") + " " + c.stringValue + " " + c.numericValue);
            }
        }
        // 보유한 능력 매칭
        String plain = hc.name.replaceAll("(?i)[§&][0-9A-FK-OR]", "").trim();
        int abc = 0;
        StringBuilder ab = new StringBuilder();
        for (HiddenAbility hab : HiddenAbility.values()) {
            if (hab.classId.equals(plain)) {
                if (abc++ > 0) ab.append(", ");
                ab.append(hab.name()).append(hab.passive ? "[P]" : "[A]");
            }
        }
        if (abc > 0) s.sendMessage("§7고유 능력: §e" + ab + " §7(P=패시브 A=액티브)");
    }

    private void showAbilities(Player p) {
        Msg.send(p, "&6=== 내 사용 가능 능력 ===");
        int n = 0;
        for (HiddenAbility ab : HiddenAbility.values()) {
            if (!plugin.abilities().owns(p, ab)) continue;
            String cd = ab.passive ? "§d패시브"
                    : (ab.cooldownMs == 0 ? "§c1회한정"
                    : (ab.cooldownMs == -1 ? "§a상시"
                    : "§e쿨 " + (ab.cooldownMs / 1000) + "초"));
            long rem = plugin.abilities().cooldownRemaining(p.getUniqueId(), ab);
            String remStr = rem > 0 ? " §c[" + (rem / 1000) + "s 남음]" : "";
            p.sendMessage("§7• §f" + ab.name() + " " + cd + " §8" + ab.description + remStr);
            n++;
        }
        if (n == 0) p.sendMessage("§7— 능력 보유 없음. 히든 클래스를 해금해야 합니다.");
    }

    private void showCooldowns(Player p) {
        Msg.send(p, "&6=== 능력 쿨다운 ===");
        int n = 0;
        for (HiddenAbility ab : HiddenAbility.values()) {
            if (!plugin.abilities().owns(p, ab)) continue;
            long rem = plugin.abilities().cooldownRemaining(p.getUniqueId(), ab);
            if (rem <= 0) continue;
            p.sendMessage("§7• §f" + ab.name() + " §c" + (rem / 1000) + "s");
            n++;
        }
        if (n == 0) p.sendMessage("§a— 모든 능력 사용 가능.");
    }
}
