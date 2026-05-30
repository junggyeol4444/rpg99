package kr.reborn.stat.growth.impl;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.event.RebornDeathEvent;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import kr.reborn.stat.growth.GrowthStrategy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 정령계 성장: 원소 친화·정령왕 호의·정수 흡수.
 *
 * 6 원소 친화도: FIRE, WATER, EARTH, WIND, LIGHT, DARK
 *   각 0~1000. 친화도 100당 해당 원소 스킬 위력 +10%.
 *   정령왕 호의 +100당 친화도 보너스 +10%.
 *
 * 정수 흡수: 같은 원소 정령 처치 시 친화도 +1
 * 정령왕 호의: 퀘스트 완료, 기도, 의식 등 외부 hook
 *
 * 위험: 정신력 0 = 소멸 (기존 동작 유지)
 *       계약 파기: 외부 onPactBreak 호출 시 SPIRIT_BETRAYAL 저주 (RebornCurse)
 */
public final class SpiritGrowth implements GrowthStrategy {

    public enum Element { FIRE, WATER, EARTH, WIND, LIGHT, DARK }

    /** uuid → element → 친화도 */
    private final Map<UUID, Map<Element, Double>> affinity = new ConcurrentHashMap<>();
    /** uuid → 원소왕(원소별)의 호의도 */
    private final Map<UUID, Map<Element, Double>> kingFavor = new ConcurrentHashMap<>();

    @Override public WorldKey world() { return WorldKey.SPIRIT; }

    @Override
    public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.SPIRIT_POWER, 0.3, "spirit-kill");
        // 무작위 원소 친화도 +1
        Element e = Element.values()[Rand.range(0, Element.values().length - 1)];
        addAffinity(p, e, 1.0);
    }

    @Override
    public void onQuestComplete(Player p, PlayerData d, double weight) {
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.SPIRIT_POWER, 4 * weight, "quest");
        // 원소왕 호의 약간
        Element e = Element.values()[Rand.range(0, Element.values().length - 1)];
        addKingFavor(p, e, 2 * weight);
    }

    @Override
    public void onMeditate(Player p, PlayerData d, double quality) {
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.SPIRIT_POWER, 5 * quality, "meditate");
        // 정신력 0 = 소멸 (기존 유지)
        if (d.getStat(StatType.MENTAL) <= 0) {
            Bukkit.getPluginManager().callEvent(
                    new RebornDeathEvent(p, p.getLocation(), null, "SPIRIT_VANISH"));
            return;
        }
        // 모든 원소 친화도 +0.5
        for (Element e : Element.values()) addAffinity(p, e, 0.5 * quality);
    }

    /** 외부 호출 — 원소 정수 흡수. */
    public void absorbEssence(Player p, Element e, double amount) {
        addAffinity(p, e, amount);
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.SPIRIT_POWER, amount * 2, "essence:" + e);
        Msg.send(p, "&5" + e + " 정수 흡수 — 친화도 +" + amount);
    }

    /** 외부 호출 — 원소왕 의식·기도. */
    public void petitionKing(Player p, Element e, double weight) {
        addKingFavor(p, e, weight);
        Msg.send(p, "&b" + e + " 원소왕에 청원 — 호의 +" + weight);
    }

    /** 외부 호출 — 계약 파기 (정령왕 배신). */
    public void onPactBreak(Player p, Element e) {
        addKingFavor(p, e, -100);
        Bukkit.broadcastMessage("§5§l[계약 파기] §f" + p.getName()
                + " §7가 " + e + " 정령왕의 신뢰를 잃었다.");
        try {
            var cp = Bukkit.getPluginManager().getPlugin("RebornCurse");
            if (cp != null) {
                Object effects = cp.getClass().getMethod("effects").invoke(cp);
                effects.getClass().getMethod("apply", Player.class, String.class)
                        .invoke(effects, p, "spirit_betrayal");
            }
        } catch (Throwable ignored) {}
    }

    /** 외부 호출 — 원소 친화도 (스킬 위력 계산용). */
    public double affinityOf(UUID p, Element e) {
        Map<Element, Double> map = affinity.get(p);
        if (map == null) return 0;
        return map.getOrDefault(e, 0.0);
    }

    public double kingFavorOf(UUID p, Element e) {
        Map<Element, Double> map = kingFavor.get(p);
        if (map == null) return 0;
        return map.getOrDefault(e, 0.0);
    }

    public Map<Element, Double> affinitiesOf(UUID p) {
        return affinity.getOrDefault(p, java.util.Collections.emptyMap());
    }

    private void addAffinity(Player p, Element e, double v) {
        Map<Element, Double> map = affinity.computeIfAbsent(p.getUniqueId(),
                k -> new EnumMap<>(Element.class));
        double next = Math.max(0, Math.min(1000, map.getOrDefault(e, 0.0) + v));
        map.put(e, next);
    }

    private void addKingFavor(Player p, Element e, double v) {
        Map<Element, Double> map = kingFavor.computeIfAbsent(p.getUniqueId(),
                k -> new EnumMap<>(Element.class));
        double next = Math.max(-200, Math.min(500, map.getOrDefault(e, 0.0) + v));
        map.put(e, next);
    }
}
