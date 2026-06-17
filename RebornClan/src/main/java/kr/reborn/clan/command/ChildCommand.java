package kr.reborn.clan.command;

import kr.reborn.clan.RebornClan;
import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class ChildCommand implements CommandExecutor {

    private static final String NS = "RebornClan.children";
    /** 자녀 자동 작명 풀 — 12개 (출생 시 무작위 1개). */
    private static final String[] CHILD_NAMES = {
            "이슬", "별빛", "강이", "들이", "달님", "온달",
            "샛별", "여울", "하늘", "바위", "노을", "솔이"
    };

    private final RebornClan plugin;
    public ChildCommand(RebornClan p) { this.plugin = p; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p) || a.length == 0) {
            Msg.send(s, "&7/child request | play | count | list | name <n> <newname> | teach <n> | visit <n> | gift <n>");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "request" -> handleRequest(p);
            case "play" -> handlePlay(p, a.length >= 2 ? a[1] : null);
            case "count" -> {
                int n = childCount(p);
                Msg.send(p, "&d보유 자녀 수: §f" + n);
            }
            case "list" -> handleList(p);
            case "name" -> { if (a.length >= 3) handleName(p, a[1], a[2]); else Msg.warn(p, "/child name <번호> <이름>"); }
            case "teach" -> { if (a.length >= 2) handleTeach(p, a[1]); else Msg.warn(p, "/child teach <번호>"); }
            case "visit" -> { if (a.length >= 2) handleVisit(p, a[1]); else Msg.warn(p, "/child visit <번호>"); }
            case "gift" -> { if (a.length >= 2) handleGift(p, a[1]); else Msg.warn(p, "/child gift <번호>"); }
            default -> Msg.warn(p, "/child request | play | count | list | name | teach | visit | gift");
        }
        return true;
    }

    /**
     * 자녀를 5분간 Villager NPC로 가시화 — 부모 옆에 spawn.
     * 자녀의 customName 표시, 부모와 가까이 있는 동안 친밀도 표현.
     * 양육과 별개의 daily 만남 — 자녀와의 시간 자체가 보상 (LUCK 60초).
     */
    private void handleVisit(Player p, String idxStr) {
        int idx;
        try { idx = Integer.parseInt(idxStr); }
        catch (NumberFormatException e) { Msg.warn(p, "번호는 숫자."); return; }
        int n = childCount(p);
        if (idx < 1 || idx > n) { Msg.error(p, "유효 번호 1~" + n); return; }
        long now = System.currentTimeMillis();
        long lastVisit = RebornCore.get().kv().getLong(NS, p.getUniqueId(),
                "child" + idx + ".lastVisit", 0);
        if (now - lastVisit < 24L * 3600_000L) {
            long h = (24L * 3600_000L - (now - lastVisit)) / 3600_000L;
            Msg.warn(p, "다음 만남까지 " + Math.max(1, h) + "시간 남음.");
            return;
        }
        RebornCore.get().kv().putLong(NS, p.getUniqueId(),
                "child" + idx + ".lastVisit", now);
        String name = RebornCore.get().kv().get(NS, p.getUniqueId(),
                "child" + idx + ".name");
        if (name == null || name.isEmpty()) name = "자녀 #" + idx;

        // Villager NPC로 spawn — 5분 후 자동 제거.
        try {
            org.bukkit.Location at = p.getLocation().add(
                    p.getLocation().getDirection().setY(0).normalize().multiply(2));
            org.bukkit.entity.Villager v = (org.bukkit.entity.Villager)
                    p.getWorld().spawnEntity(at, org.bukkit.entity.EntityType.VILLAGER);
            v.setCustomName("§d" + name + " §7(" + p.getName() + "의 자녀)");
            v.setCustomNameVisible(true);
            v.setAI(true);
            v.setRemoveWhenFarAway(true);
            // 5분 후 자동 제거
            kr.reborn.core.RebornCore.get().scheduler().runTaskLater(() -> {
                try {
                    if (v.isValid() && !v.isDead()) {
                        v.getWorld().spawnParticle(org.bukkit.Particle.HEART,
                                v.getLocation().add(0, 1, 0), 10);
                        v.remove();
                    }
                } catch (Throwable ignored) {}
            }, 20L * 60 * 5);
            // 부모에게 LUCK 60초 (자녀와의 시간 보상)
            p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.LUCK, 20 * 60, 0, true, false));
            Msg.send(p, "&d" + name + " §7이(가) 만나러 왔다 — 5분간 머무름.");
        } catch (Throwable t) {
            Msg.error(p, "자녀 spawn 실패: " + t.getMessage());
        }
    }

    /**
     * 자녀에게 선물 — 메인 손 아이템 1개 소모, 24h 1회.
     * 선물 받은 자녀의 parentTotal +200 (양육보다 효과 큼 — 실제 선물의 가치).
     */
    private void handleGift(Player p, String idxStr) {
        int idx;
        try { idx = Integer.parseInt(idxStr); }
        catch (NumberFormatException e) { Msg.warn(p, "번호는 숫자."); return; }
        int n = childCount(p);
        if (idx < 1 || idx > n) { Msg.error(p, "유효 번호 1~" + n); return; }
        org.bukkit.inventory.ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || item.getType() == org.bukkit.Material.AIR) {
            Msg.error(p, "메인 손에 선물 아이템을 들어라."); return;
        }
        long now = System.currentTimeMillis();
        long lastGift = RebornCore.get().kv().getLong(NS, p.getUniqueId(),
                "child" + idx + ".lastGift", 0);
        if (now - lastGift < 24L * 3600_000L) {
            long h = (24L * 3600_000L - (now - lastGift)) / 3600_000L;
            Msg.warn(p, "다음 선물까지 " + Math.max(1, h) + "시간 남음.");
            return;
        }
        // 1개 소모
        item.setAmount(item.getAmount() - 1);
        p.getInventory().setItemInMainHand(item);
        RebornCore.get().kv().putLong(NS, p.getUniqueId(),
                "child" + idx + ".lastGift", now);
        double cur = RebornCore.get().kv().getDouble(NS, p.getUniqueId(),
                "child" + idx + ".parentTotal", 0);
        double next = cur + 200;
        RebornCore.get().kv().putDouble(NS, p.getUniqueId(),
                "child" + idx + ".parentTotal", next);
        String name = RebornCore.get().kv().get(NS, p.getUniqueId(),
                "child" + idx + ".name");
        if (name == null) name = "#" + idx;
        Msg.send(p, "&d" + name + " §a이(가) 선물(§f" + item.getType().name()
                + "§a)을 받고 기뻐한다 — 인수 보정 +200 (모든 스탯 +1.25).");
    }

    private void handleRequest(Player p) {
        if (plugin.marriages().of(p.getUniqueId()) == null) {
            Msg.error(p, "결혼한 상태여야 한다.");
            return;
        }
        // 일일 1회 제한 — 무한 임신 시도 차단
        long now = System.currentTimeMillis();
        long last = RebornCore.get().kv().getLong(NS, p.getUniqueId(), "lastTry", 0);
        if (now - last < 24L * 3600_000L) {
            long h = (24L * 3600_000L - (now - last)) / 3600_000L;
            Msg.warn(p, "다음 시도까지 " + Math.max(1, h) + "시간 남음.");
            return;
        }
        RebornCore.get().kv().putLong(NS, p.getUniqueId(), "lastTry", now);
        double chance = plugin.getConfig().getDouble("child.request-success-chance", 0.30);
        if (Rand.chance(chance)) {
            int n = childCount(p) + 1;
            RebornCore.get().kv().putInt(NS, p.getUniqueId(), "count", n);
            RebornCore.get().kv().putLong(NS, p.getUniqueId(), "child" + n + ".bornAt", now);
            // 자동 작명 — 무작위 이름 풀에서 선택.
            String name = CHILD_NAMES[Rand.range(0, CHILD_NAMES.length - 1)];
            RebornCore.get().kv().put(NS, p.getUniqueId(), "child" + n + ".name", name);
            // 부모 스탯 스냅샷 (자녀로 전환 시 5% 보정에 사용)
            PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
            if (d != null) {
                double total = RebornCore.get().api().getTotalStats(p.getUniqueId());
                RebornCore.get().kv().putDouble(NS, p.getUniqueId(),
                        "child" + n + ".parentTotal", total);
            }
            Bukkit.broadcastMessage("§d§l[출생] §f" + p.getName() + "의 자녀 §6" + name
                    + " §f탄생! (" + n + "번째)");
            Msg.send(p, "&d자녀 " + name + " 태어남. §7/child name " + n
                    + " <이름> 으로 개명, /child teach " + n + " 로 양육 가능.");
        } else {
            Msg.warn(p, "이번에는 임신되지 않았다.");
        }
    }

    /** 자녀 목록 — 번호·이름·나이·인수받을 stat 보정. */
    private void handleList(Player p) {
        int n = childCount(p);
        if (n == 0) { Msg.send(p, "&7자녀 없음. /child request 로 시도."); return; }
        Msg.send(p, "&d=== 자녀 " + n + "명 ===");
        long now = System.currentTimeMillis();
        for (int i = 1; i <= n; i++) {
            String name = RebornCore.get().kv().get(NS, p.getUniqueId(), "child" + i + ".name");
            if (name == null || name.isEmpty()) name = "(이름없음)";
            long bornAt = RebornCore.get().kv().getLong(NS, p.getUniqueId(),
                    "child" + i + ".bornAt", now);
            double total = RebornCore.get().kv().getDouble(NS, p.getUniqueId(),
                    "child" + i + ".parentTotal", 0);
            long days = (now - bornAt) / (24L * 3600_000L);
            p.sendMessage("§d#" + i + " §f" + name + " §7 (나이 " + days
                    + "일, 인수 보정 ≈ " + String.format("%.1f", total * 0.05 / 8) + "/스탯)");
        }
    }

    private void handleName(Player p, String idxStr, String newName) {
        int idx;
        try { idx = Integer.parseInt(idxStr); }
        catch (NumberFormatException e) { Msg.warn(p, "번호는 숫자."); return; }
        int n = childCount(p);
        if (idx < 1 || idx > n) { Msg.error(p, "유효 번호 1~" + n); return; }
        if (newName.length() > 20) { Msg.warn(p, "이름 20자 이내."); return; }
        RebornCore.get().kv().put(NS, p.getUniqueId(), "child" + idx + ".name", newName);
        Msg.send(p, "&d자녀 #" + idx + " 이름을 §f" + newName + "&d 으로 바꿨다.");
    }

    /**
     * 양육 — 매 자녀당 24h 1회. 자녀의 parentTotal +1% (인수 시 더 강력해짐).
     * 부모 양육이 누적되면 자녀 세대로 전환 시 보정이 점점 늘어남.
     */
    private void handleTeach(Player p, String idxStr) {
        int idx;
        try { idx = Integer.parseInt(idxStr); }
        catch (NumberFormatException e) { Msg.warn(p, "번호는 숫자."); return; }
        int n = childCount(p);
        if (idx < 1 || idx > n) { Msg.error(p, "유효 번호 1~" + n); return; }
        long now = System.currentTimeMillis();
        long lastTeach = RebornCore.get().kv().getLong(NS, p.getUniqueId(),
                "child" + idx + ".lastTeach", 0);
        if (now - lastTeach < 24L * 3600_000L) {
            long h = (24L * 3600_000L - (now - lastTeach)) / 3600_000L;
            Msg.warn(p, "다음 양육까지 " + Math.max(1, h) + "시간 남음.");
            return;
        }
        RebornCore.get().kv().putLong(NS, p.getUniqueId(),
                "child" + idx + ".lastTeach", now);
        double cur = RebornCore.get().kv().getDouble(NS, p.getUniqueId(),
                "child" + idx + ".parentTotal", 0);
        double bonus = Math.max(50, cur * 0.01);  // 최소 +50, 최대 1%
        double next = cur + bonus;
        RebornCore.get().kv().putDouble(NS, p.getUniqueId(),
                "child" + idx + ".parentTotal", next);
        String name = RebornCore.get().kv().get(NS, p.getUniqueId(),
                "child" + idx + ".name");
        if (name == null) name = "#" + idx;
        Msg.send(p, "&d" + name + " §a양육 +" + String.format("%.1f", bonus)
                + " (인수 시 모든 스탯 +" + String.format("%.2f", bonus * 0.05 / 8) + ")");
    }

    private void handlePlay(Player p, String idxStr) {
        int count = childCount(p);
        if (count == 0) {
            Msg.error(p, "자녀가 없다. /child request 로 자녀를 두고 다시 시도하라.");
            return;
        }
        // 인수받을 자녀 번호 — 미지정 시 가장 최근(count번째). 명시 시 검증.
        int target = count;
        if (idxStr != null) {
            try {
                int n = Integer.parseInt(idxStr);
                if (n < 1 || n > count) { Msg.error(p, "유효 번호 1~" + count); return; }
                target = n;
            } catch (NumberFormatException e) { Msg.warn(p, "번호는 숫자."); return; }
        }
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (d == null) { Msg.error(p, "데이터 로드 실패."); return; }
        // 현재 캐릭터 은퇴 → 과거생 기록
        try { RebornCore.get().reincarnationMemory().recordPastLife(p, "CHILD_INHERITANCE"); }
        catch (Throwable ignored) {}
        double parentTotal = RebornCore.get().kv().getDouble(NS, p.getUniqueId(),
                "child" + target + ".parentTotal", 0);
        String childName = RebornCore.get().kv().get(NS, p.getUniqueId(),
                "child" + target + ".name");
        if (childName == null || childName.isEmpty()) childName = "자녀 #" + target;
        // 자녀 스탯 = 1 초기 + 부모 총합의 5% 가산 (기획서 23장 + 양육 누적)
        for (StatType t : StatType.COMMON_8) d.setStat(t, 1);
        double perStat = (parentTotal / 8.0) * 0.05;
        if (perStat > 0) {
            for (StatType t : StatType.COMMON_8) d.addStat(t, perStat);
        }
        d.reincarnations(d.reincarnations() + 1);
        d.tier("");
        d.titleId("");
        d.gymUsed(false);
        d.childStart(true);
        // 선택된 자녀 사용 처리 — 뒤쪽 자녀가 있으면 앞으로 시프트.
        for (int i = target; i < count; i++) {
            shiftChildKey(p, i + 1, i);
        }
        // 마지막 슬롯 클리어
        clearChildKey(p, count);
        RebornCore.get().kv().putInt(NS, p.getUniqueId(), "count", count - 1);
        Bukkit.broadcastMessage("§d§l[자녀 전환] §f" + p.getName()
                + "이(가) §6" + childName + " §f으로 세대를 이어받았다!");
        Msg.send(p, "&d" + childName + " 으로 전환 — 부모 총합의 5% 보정 (+"
                + String.format("%.1f", perStat) + " per 스탯).");
        var lobby = Bukkit.getWorld("lobby");
        if (lobby != null) p.teleport(lobby.getSpawnLocation());
    }

    private void shiftChildKey(Player p, int from, int to) {
        for (String field : new String[]{"name", "bornAt", "parentTotal", "lastTeach"}) {
            String v = RebornCore.get().kv().get(NS, p.getUniqueId(), "child" + from + "." + field);
            if (v != null) RebornCore.get().kv().put(NS, p.getUniqueId(), "child" + to + "." + field, v);
        }
    }

    private void clearChildKey(Player p, int idx) {
        for (String field : new String[]{"name", "bornAt", "parentTotal", "lastTeach"}) {
            RebornCore.get().kv().remove(NS, p.getUniqueId(), "child" + idx + "." + field);
        }
    }

    private int childCount(Player p) {
        return RebornCore.get().kv().getInt(NS, p.getUniqueId(), "count", 0);
    }
}
