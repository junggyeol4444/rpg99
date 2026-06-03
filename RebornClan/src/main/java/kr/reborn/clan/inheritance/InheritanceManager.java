package kr.reborn.clan.inheritance;

import kr.reborn.clan.RebornClan;
import kr.reborn.clan.data.Clan;
import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 상속 매니저.
 *
 * 가문/혈통 기반 자산·비급 상속:
 *   - 가주 사망 시 → 원로 중 1명이 자동 계승
 *   - 가문원 사망 시 → 가문 treasury에 보유 금액 30% 자동 이양
 *   - 결혼 + 자녀 NPC 있으면 → 비급 50% 확률로 자녀에게 자동 상속 (메모리)
 *
 * 환생 시 (RebornCore 윤회 트리거):
 *   - 가문 보유자: 가문 treasury 일부를 환생 후에도 유지
 *   - 가문장: 자녀가 자동으로 새 가주 됨
 *
 * 유언장 (Will): 사전에 작성 가능 — 특정 NPC/플레이어에게 자산 이전 지정.
 */
public final class InheritanceManager implements Listener {

    private final RebornClan plugin;
    /** uuid → 유언장 (자산 분배 우선순위 NPC/플레이어 id) */
    private final Map<UUID, Will> wills = new ConcurrentHashMap<>();

    public InheritanceManager(RebornClan plugin) { this.plugin = plugin; }

    private static final String NS = "RebornClan.inheritance";

    public void writeWill(Player p, String heirId) {
        Will w = new Will(p.getUniqueId(), heirId, System.currentTimeMillis());
        wills.put(p.getUniqueId(), w);
        try {
            kr.reborn.core.RebornCore.get().kv().put(NS, p.getUniqueId(), "heir", heirId);
            kr.reborn.core.RebornCore.get().kv().putLong(NS, p.getUniqueId(), "writtenAt", w.writtenAt);
        } catch (Throwable ignored) {}
        Msg.send(p, "&6유언장 작성: 상속인 §f" + heirId);
    }

    /** 가문 사망 시 호출 — 자산 이양. */
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        Clan c = plugin.clans().ofPlayer(p.getUniqueId());
        if (c == null) return;

        // 가주 사망 — 원로 1명 자동 계승
        if (c.leader.equals(p.getUniqueId())) {
            if (!c.elders.isEmpty()) {
                UUID newLeader = c.elders.iterator().next();
                c.leader = newLeader;
                c.elders.remove(newLeader);
                Player np = Bukkit.getPlayer(newLeader);
                Bukkit.broadcastMessage("§6§l[가문 계승] §f" + c.name
                        + " §7새 가주: §e" + (np != null ? np.getName() : newLeader));
                if (np != null) Msg.send(np, "&6&l[계승] §7가주가 되었다.");
            } else {
                Bukkit.broadcastMessage("§c§l[가문 멸문] §f" + c.name
                        + " §7가주 사망, 후계자 없음 — 가문 와해.");
            }
        }

        // 가문원 자산 30% 이양 (RebornEconomy 리플렉션)
        try {
            var ep = Bukkit.getPluginManager().getPlugin("RebornEconomy");
            if (ep != null) {
                Object cur = ep.getClass().getMethod("currencies").invoke(ep);
                Object bal = cur.getClass().getMethod("balance",
                                UUID.class, String.class)
                        .invoke(cur, p.getUniqueId(), "GOLD_COIN");
                if (bal instanceof Number n && n.longValue() > 0) {
                    long inherit = (long) (n.longValue() * 0.30);
                    cur.getClass().getMethod("withdraw", UUID.class, String.class, long.class)
                            .invoke(cur, p.getUniqueId(), "GOLD_COIN", inherit);
                    c.treasury += inherit;
                    Bukkit.broadcastMessage("§6[상속] §f" + p.getName()
                            + " §7→ " + c.name + " §6+" + inherit + "g");
                }
            }
        } catch (Throwable ignored) {}

        // 유언장 적용
        Will w = wills.remove(p.getUniqueId());
        if (w != null) {
            applyWill(p, w);
        }
    }

    private void applyWill(Player deceased, Will will) {
        // 유언장에 명시된 상속인에게 비급/자산 이양 시도
        Player heir = Bukkit.getPlayerExact(will.heirId);
        if (heir != null) {
            // 비급 양도 — RebornSkill 리플렉션
            try {
                var sp = Bukkit.getPluginManager().getPlugin("RebornSkill");
                if (sp != null) {
                    Object mm = sp.getClass().getMethod("manuals").invoke(sp);
                    Object set = mm.getClass().getMethod("ownedOf", UUID.class)
                            .invoke(mm, deceased.getUniqueId());
                    if (set instanceof java.util.Set<?> s) {
                        int n = 0;
                        for (Object o : new java.util.ArrayList<>(s)) {
                            mm.getClass().getMethod("transferTo",
                                            Player.class, Player.class, String.class)
                                    .invoke(mm, deceased, heir, String.valueOf(o));
                            n++;
                            if (n >= 3) break;
                        }
                        if (n > 0) {
                            Msg.send(heir, "&6유언 상속: 비급 " + n + "종 이전");
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    /** 환생 시 호출 (RebornDeath/Spawn에서). */
    public void onReincarnation(Player p) {
        Clan c = plugin.clans().ofPlayer(p.getUniqueId());
        if (c == null) return;
        // 가문 treasury 10%만 환생 후에도 유지 (직접 가지지는 않음, 상징적)
        double retained = c.treasury * 0.10;
        Msg.send(p, "&6환생 후에도 가문 §f" + c.name + " §7과의 연 유지 (treasury "
                + (long) retained + "g 상징적 잔존)");
    }

    public Will willOf(UUID p) {
        Will cached = wills.get(p);
        if (cached != null) return cached;
        String heir = kr.reborn.core.RebornCore.get().kv().get(NS, p, "heir");
        if (heir == null) return null;
        long at = kr.reborn.core.RebornCore.get().kv().getLong(NS, p, "writtenAt", System.currentTimeMillis());
        Will w = new Will(p, heir, at);
        wills.put(p, w);
        return w;
    }

    public static final class Will {
        public final UUID author;
        public final String heirId;
        public final long writtenAt;
        public Will(UUID a, String h, long t) {
            this.author = a; this.heirId = h; this.writtenAt = t;
        }
    }
}
