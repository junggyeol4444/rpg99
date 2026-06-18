package kr.reborn.pet.manager;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.pet.RebornPet;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 정령계 핵심 계약 시스템 + 일반 계약.
 * 등급: HALF / MEDIUM / HIGH / SOUL
 */
public final class ContractManager {

    private static final String NS = "RebornPet.contract";

    private final RebornPet plugin;
    private final Map<UUID, Contract> contracts = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> loaded = ConcurrentHashMap.newKeySet();

    public ContractManager(RebornPet p) { this.plugin = p; }

    /** KV lazy 로드 — 재시작으로 계약이 사라져 파기 페널티를 회피하던 구멍 봉쇄. */
    private void ensureLoaded(UUID owner) {
        if (loaded.add(owner)) {
            try {
                String partner = RebornCore.get().kv().get(NS, owner, "partner");
                if (partner == null || partner.isEmpty()) return;
                Grade g = Grade.HALF;
                try { g = Grade.valueOf(RebornCore.get().kv().get(NS, owner, "grade")); }
                catch (Throwable ignored) {}
                contracts.put(owner, new Contract(partner, g));
            } catch (Throwable ignored) {}
        }
    }

    public boolean propose(Player owner, String targetMobOrSpiritId, Grade grade) {
        ensureLoaded(owner.getUniqueId());
        if (contracts.containsKey(owner.getUniqueId())) {
            Msg.error(owner, "이미 계약 중"); return false;
        }
        contracts.put(owner.getUniqueId(), new Contract(targetMobOrSpiritId, grade));
        try {
            RebornCore.get().kv().put(NS, owner.getUniqueId(), "partner", targetMobOrSpiritId);
            RebornCore.get().kv().put(NS, owner.getUniqueId(), "grade", grade.name());
        } catch (Throwable ignored) {}
        Msg.send(owner, "&b계약 성립: " + targetMobOrSpiritId + " (" + grade + ")");
        return true;
    }

    public void breakContract(Player owner) {
        ensureLoaded(owner.getUniqueId());
        Contract c = contracts.remove(owner.getUniqueId());
        if (c == null) return;
        try {
            RebornCore.get().kv().remove(NS, owner.getUniqueId(), "partner");
            RebornCore.get().kv().remove(NS, owner.getUniqueId(), "grade");
        } catch (Throwable ignored) {}
        PlayerData d = RebornCore.get().api().getPlayerData(owner.getUniqueId());
        if (d == null) return;
        if (d.worldKey() == WorldKey.SPIRIT) {
            // 정령력 50% 영구 감소
            double cur = d.getStat(StatType.SPIRIT_POWER);
            d.setStat(StatType.SPIRIT_POWER, cur * (1 - plugin.getConfig().getDouble("contract.spirit-spirit-power-loss-percent", 50) / 100.0));
            Msg.error(owner, "&5계약 파기 — 정령력 50% 영구 감소.");
            // SpiritGrowth 정령왕 호의 하락 연동
            try {
                var sp = org.bukkit.Bukkit.getPluginManager().getPlugin("RebornStat");
                if (sp != null) {
                    Object growth = sp.getClass().getMethod("growth").invoke(sp);
                    Object strategy = growth.getClass().getMethod("of", WorldKey.class)
                            .invoke(growth, WorldKey.SPIRIT);
                    if (strategy != null) {
                        // 계약 partner가 원소명이면 해당 원소왕 호의 -100
                        Class<?> elCls = Class.forName("kr.reborn.stat.growth.impl.SpiritGrowth$Element");
                        for (Object el : elCls.getEnumConstants()) {
                            if (el.toString().equalsIgnoreCase(c.partnerId)) {
                                strategy.getClass().getMethod("onPactBreak", Player.class, elCls)
                                        .invoke(strategy, owner, el);
                                break;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        } else {
            double pct = plugin.getConfig().getDouble("contract.break-stat-loss-percent", 10) / 100.0;
            for (StatType t : StatType.COMMON_8) d.setStat(t, d.getStat(t) * (1 - pct));
            Msg.error(owner, "계약 파기 — 모든 스탯 " + (pct * 100) + "% 감소.");
        }
    }

    public Contract of(UUID owner) { ensureLoaded(owner); return contracts.get(owner); }

    public enum Grade { HALF, MEDIUM, HIGH, SOUL }

    public static final class Contract {
        public final String partnerId;
        public Grade grade;
        public Contract(String partnerId, Grade grade) {
            this.partnerId = partnerId; this.grade = grade;
        }
    }
}
