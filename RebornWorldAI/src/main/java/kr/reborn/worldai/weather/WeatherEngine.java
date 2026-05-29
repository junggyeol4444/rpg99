package kr.reborn.worldai.weather;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.worldai.RebornWorldAI;
import kr.reborn.worldai.history.WorldHistory;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumMap;
import java.util.Map;

/**
 * 날씨 실행 엔진.
 *
 * 활성 커스텀 날씨를 추적, 같은 세계 플레이어에게 effect 필드에 정의된 효과 적용.
 *
 * 지원 effect:
 *   DEMON_KI_BOOST    — DEMON_KI +mult
 *   BLOOD_DAMAGE      — HP -1 매 사이클
 *   FIRE_EVERYWHERE   — FIRE_TICKS 200
 *   POISON_FOG        — POISON 0
 *   INNER_KI_DRAIN    — INNER_KI -mult
 *   EARTHQUAKE        — SLOW 1
 *   SPIRIT_POWER_BOOST/HEAVEN_KI_BOOST/YOKAI_KI_BOOST/OCEAN_POWER_BOOST/MAGITECH_ENERGY_BOOST 동일 패턴
 *   MEDITATE_BOOST    — 수련 효율 +mult (RebornStat hook)
 *   STAT_PENALTY_MAJOR — COMMON 8 스탯 -1
 *   STAT_PENALTY_MINOR — COMMON 8 스탯 -0.5
 *   DROP_BOOST        — LUCK +mult * 100
 *   MANA_BOOST/DRAIN  — MANA +/- mult * 20
 *   RADIATION_DAMAGE  — HP -1 + WITHER
 *   VISIBILITY_LOSS   — BLINDNESS
 *   HACK_BOOST        — INTELLIGENCE +mult * 10
 *   CYBER_DEGRADATION — CYBER_ADAPTATION -2
 *   PRIMORDIAL_AURA   — SPIRIT_POWER +50 + 모든 능력 보강
 *   기타: 단순 broadcast
 */
public final class WeatherEngine {

    private final RebornWorldAI plugin;
    private final Map<WorldKey, ActiveWeather> active = new EnumMap<>(WorldKey.class);

    public WeatherEngine(RebornWorldAI plugin) {
        this.plugin = plugin;
        RebornCore.get().scheduler().runTimer(this::tick, 200L, 200L); // 10초마다
    }

    public void start(WorldKey w, String name, int durationMin) {
        var weatherSec = plugin.getConfig().getConfigurationSection("weathers." + name);
        if (weatherSec == null) return;
        String effect = weatherSec.getString("effect", "NONE");
        double mult = weatherSec.getDouble("mult", 1.0);
        ActiveWeather aw = new ActiveWeather(w, name, effect, mult,
                System.currentTimeMillis() + durationMin * 60_000L);
        active.put(w, aw);
        Bukkit.broadcastMessage("§b§l[" + w + " 날씨] §f" + name
                + " §7| 효과 " + effect + " (" + durationMin + "분)");
        plugin.history().record(w, WorldHistory.EventKind.WEATHER, name + ":" + effect);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (var it = active.entrySet().iterator(); it.hasNext();) {
            var en = it.next();
            ActiveWeather aw = en.getValue();
            if (now > aw.endsAt) {
                it.remove();
                Bukkit.broadcastMessage("§7[" + aw.world + " 날씨 종료] " + aw.name);
                continue;
            }
            applyToPlayers(aw);
        }
    }

    private void applyToPlayers(ActiveWeather aw) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                var data = RebornCore.get().api().getPlayerData(p.getUniqueId());
                if (data == null || data.worldKey() != aw.world) continue;
                applyEffect(p, aw);
            } catch (Throwable ignored) {}
        }
    }

    private void applyEffect(Player p, ActiveWeather aw) {
        switch (aw.effect) {
            case "DEMON_KI_BOOST" -> addStat(p, StatType.DEMON_KI, 10 * aw.mult, aw.effect);
            case "BLOOD_DAMAGE" -> { try { p.damage(1); } catch (Throwable ignored) {} }
            case "FIRE_EVERYWHERE" -> { try { p.setFireTicks(200); } catch (Throwable ignored) {} }
            case "POISON_FOG" -> potion(p, PotionEffectType.POISON, 200, 0);
            case "INNER_KI_DRAIN" -> addStat(p, StatType.INNER_KI, -5 * aw.mult, aw.effect);
            case "EARTHQUAKE" -> potion(p, PotionEffectType.SLOW, 200, 1);
            case "SPIRIT_POWER_BOOST" -> addStat(p, StatType.SPIRIT_POWER, 10 * aw.mult, aw.effect);
            case "RANDOM_ELEMENT" -> addStat(p, StatType.SPIRIT_POWER, 5, aw.effect);
            case "PRIMORDIAL_AURA" -> {
                addStat(p, StatType.SPIRIT_POWER, 50, aw.effect);
                potion(p, PotionEffectType.INCREASE_DAMAGE, 240, 2);
            }
            case "MANA_BOOST" -> addStat(p, StatType.MANA, 20 * aw.mult, aw.effect);
            case "MANA_DRAIN" -> addStat(p, StatType.MANA, -20 * aw.mult, aw.effect);
            case "METEOR_RAIN" -> {
                if (Math.random() < 0.05) try {
                    p.getWorld().createExplosion(p.getLocation().clone().add(
                            (Math.random() - 0.5) * 20, 0, (Math.random() - 0.5) * 20),
                            2.0f, false, false);
                } catch (Throwable ignored) {}
            }
            case "HEAVEN_KI_BOOST" -> addStat(p, StatType.HEAVEN_KI, 10 * aw.mult, aw.effect);
            case "TAINT_RISK" -> {
                if (Math.random() < 0.02) {
                    try {
                        var cp = Bukkit.getPluginManager().getPlugin("RebornCurse");
                        if (cp != null) {
                            cp.getClass().getMethod("effects").invoke(cp)
                                    .getClass().getMethod("apply", Player.class, String.class)
                                    .invoke(cp.getClass().getMethod("effects").invoke(cp),
                                            p, "monk_oath_break");
                        }
                    } catch (Throwable ignored) {}
                }
            }
            case "TRIBULATION_RISK" -> {
                if (Math.random() < 0.005) try { p.damage(20); } catch (Throwable ignored) {}
            }
            case "MEDITATE_BOOST" -> potion(p, PotionEffectType.LUCK, 240, 2);
            case "YOKAI_KI_BOOST" -> addStat(p, StatType.YOKAI_KI, 10 * aw.mult, aw.effect);
            case "ILLUSION_RISK" -> potion(p, PotionEffectType.CONFUSION, 100, 0);
            case "HOSTILE_YOKAI_SPAWN" -> {
                if (Math.random() < 0.02) try {
                    p.getWorld().spawnEntity(p.getLocation().clone().add(5, 0, 5),
                            org.bukkit.entity.EntityType.PHANTOM);
                } catch (Throwable ignored) {}
            }
            case "GATE_OUTBREAK_RISK" -> addStat(p, StatType.STRENGTH, -1, aw.effect);
            case "STAT_PENALTY_MINOR" -> {
                for (StatType t : StatType.COMMON_8) addStat(p, t, -0.5, aw.effect);
            }
            case "STAT_PENALTY_MAJOR" -> {
                for (StatType t : StatType.COMMON_8) addStat(p, t, -1, aw.effect);
            }
            case "MAGITECH_ENERGY_BOOST" -> addStat(p, StatType.MAGITECH_ENERGY, 10 * aw.mult, aw.effect);
            case "GOLEM_RAMPAGE" -> potion(p, PotionEffectType.SLOW, 100, 0);
            case "RADIATION_DAMAGE" -> {
                try { p.damage(1); } catch (Throwable ignored) {}
                potion(p, PotionEffectType.WITHER, 100, 0);
            }
            case "VISIBILITY_LOSS" -> potion(p, PotionEffectType.BLINDNESS, 200, 0);
            case "MUTANT_SPAWN_BOOST" -> {
                if (Math.random() < 0.01) try {
                    p.getWorld().spawnEntity(p.getLocation().clone().add(10, 0, 10),
                            org.bukkit.entity.EntityType.ZOMBIE);
                } catch (Throwable ignored) {}
            }
            case "CYBER_DEGRADATION" -> addStat(p, StatType.CYBER_ADAPTATION, -2, aw.effect);
            case "HACK_BOOST" -> addStat(p, StatType.INTELLIGENCE, 10 * aw.mult, aw.effect);
            case "DROP_BOOST" -> potion(p, PotionEffectType.LUCK, 240, (int) (aw.mult * 2));
            case "TSUNAMI_DAMAGE" -> {
                if (p.getLocation().getBlock().isLiquid()) {
                    try { p.damage(2); } catch (Throwable ignored) {}
                }
            }
            case "OCEAN_POWER_BOOST" -> addStat(p, StatType.OCEAN_POWER, 10 * aw.mult, aw.effect);
            case "GHOST_SHIP_SPAWN" -> {
                if (Math.random() < 0.01) try {
                    p.getWorld().spawnParticle(Particle.SPELL_WITCH, p.getLocation(), 100, 5, 1, 5);
                } catch (Throwable ignored) {}
            }
            default -> { /* 알 수 없는 효과 */ }
        }
    }

    private void addStat(Player p, StatType t, double v, String src) {
        try { RebornCore.get().api().addStat(p.getUniqueId(), t, v, "WEATHER:" + src); }
        catch (Throwable ignored) {}
    }

    private void potion(Player p, PotionEffectType type, int dur, int amp) {
        try { p.addPotionEffect(new PotionEffect(type, dur, amp)); }
        catch (Throwable ignored) {}
    }

    public Map<WorldKey, ActiveWeather> activeAll() { return active; }

    public static final class ActiveWeather {
        public final WorldKey world;
        public final String name;
        public final String effect;
        public final double mult;
        public final long endsAt;

        public ActiveWeather(WorldKey w, String n, String e, double m, long endsAt) {
            this.world = w; this.name = n; this.effect = e; this.mult = m; this.endsAt = endsAt;
        }
    }
}
