package kr.reborn.worldai.disaster;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Rand;
import kr.reborn.worldai.RebornWorldAI;
import kr.reborn.worldai.history.WorldHistory;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumMap;
import java.util.Map;

/**
 * 재해 실행 엔진.
 *
 * RebornDisasterStartEvent를 받아 활성 재해를 추적. 매 tick에:
 * - earthquake: 같은 세계 플레이어 점프 + 데미지
 * - tsunami: 해양/해안 플레이어 강제 수영 + 슬로우 + 데미지
 * - mana_burst: 마법 세계 마나 +대량, 마법사 강화
 * - ley_break: 마법 세계 마나 -대량, 모든 마법 사용 불가
 * - dust_storm: 황무지 시야 차단 + 행동 둔화
 * - meteor_shower: 무작위 위치에 폭발
 *
 * 모든 재해는 RebornDisasterStartEvent의 durationSeconds 후 자동 종료.
 */
public final class DisasterEngine {

    private final RebornWorldAI plugin;
    private final Map<WorldKey, ActiveDisaster> active = new EnumMap<>(WorldKey.class);

    public DisasterEngine(RebornWorldAI plugin) {
        this.plugin = plugin;
        RebornCore.get().scheduler().runTimer(this::tick, 100L, 100L); // 5초마다
    }

    public void start(WorldKey w, String type, int durationSeconds) {
        if (active.containsKey(w)) return; // 이미 활성
        ActiveDisaster d = new ActiveDisaster(w, type, System.currentTimeMillis() + durationSeconds * 1000L);
        active.put(w, d);
        Bukkit.broadcastMessage("§4§l[" + w + " 재해] §f" + labelOf(type) + " §c발생! "
                + (durationSeconds / 60) + "분간 지속.");
        plugin.history().record(w, WorldHistory.EventKind.DISASTER, type);
    }

    public void stop(WorldKey w) {
        ActiveDisaster d = active.remove(w);
        if (d == null) return;
        Bukkit.broadcastMessage("§6[" + w + " 재해 종료] " + labelOf(d.type));
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (var it = active.entrySet().iterator(); it.hasNext();) {
            var en = it.next();
            ActiveDisaster d = en.getValue();
            if (now > d.endsAt) {
                it.remove();
                Bukkit.broadcastMessage("§6[" + d.world + " 재해 종료] " + labelOf(d.type));
                continue;
            }
            applyToPlayers(d);
        }
    }

    private void applyToPlayers(ActiveDisaster d) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                var data = RebornCore.get().api().getPlayerData(p.getUniqueId());
                if (data == null || data.worldKey() != d.world) continue;
                applyEffect(p, d.type);
            } catch (Throwable ignored) {}
        }
    }

    private void applyEffect(Player p, String type) {
        switch (type) {
            case "earthquake" -> {
                try {
                    p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector(
                            Rand.rangeD(-0.3, 0.3), 0.4, Rand.rangeD(-0.3, 0.3))));
                    p.damage(2);
                    p.getWorld().playSound(p.getLocation(), Sound.BLOCK_DEEPSLATE_BREAK, 1.0f, 0.5f);
                    p.getWorld().spawnParticle(Particle.BLOCK_DUST, p.getLocation(), 50, 5, 0.5, 5,
                            org.bukkit.Material.DIRT.createBlockData());
                } catch (Throwable ignored) {}
            }
            case "tsunami" -> {
                try {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 2));
                    if (p.getLocation().getBlock().isLiquid()) {
                        p.damage(3);
                        p.setVelocity(p.getVelocity().add(new org.bukkit.util.Vector(0.5, 0, 0)));
                    }
                } catch (Throwable ignored) {}
            }
            case "mana_burst" -> {
                try {
                    RebornCore.get().api().addStat(p.getUniqueId(),
                            kr.reborn.core.data.StatType.MANA, 50, "DISASTER:mana_burst");
                    p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 200, 1));
                } catch (Throwable ignored) {}
            }
            case "ley_break" -> {
                try {
                    RebornCore.get().api().addStat(p.getUniqueId(),
                            kr.reborn.core.data.StatType.MANA, -30, "DISASTER:ley_break");
                    p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 1));
                } catch (Throwable ignored) {}
            }
            case "dust_storm" -> {
                try {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 1));
                } catch (Throwable ignored) {}
            }
            case "meteor_shower" -> {
                if (Rand.chance(0.1)) {
                    try {
                        p.getWorld().createExplosion(p.getLocation().clone().add(
                                Rand.rangeD(-10, 10), 0, Rand.rangeD(-10, 10)),
                                2.0f, false, false);
                    } catch (Throwable ignored) {}
                }
            }
            default -> { /* 알 수 없는 재해 — 무시 */ }
        }
    }

    public String labelOf(String type) {
        switch (type) {
            case "earthquake": return "대지진";
            case "tsunami": return "쓰나미";
            case "mana_burst": return "마나 폭발";
            case "ley_break": return "마나 고갈";
            case "dust_storm": return "흑색 모래폭풍";
            case "meteor_shower": return "유성우";
        }
        return type;
    }

    public Map<WorldKey, ActiveDisaster> activeAll() { return active; }

    public static final class ActiveDisaster {
        public final WorldKey world;
        public final String type;
        public final long endsAt;

        public ActiveDisaster(WorldKey w, String t, long e) {
            this.world = w; this.type = t; this.endsAt = e;
        }
    }
}
