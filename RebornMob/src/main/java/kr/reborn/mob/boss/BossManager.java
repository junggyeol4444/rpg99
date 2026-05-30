package kr.reborn.mob.boss;

import kr.reborn.core.RebornCore;
import kr.reborn.mob.RebornMob;
import kr.reborn.mob.def.MobDef;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BossManager {

    private final RebornMob plugin;
    private final Map<UUID, BossInstance> active = new HashMap<>();
    private final PatternEngine patternEngine = new PatternEngine();

    public BossManager(RebornMob p) {
        this.plugin = p;
        // 2초마다 모든 보스의 페이즈/패턴 갱신
        RebornCore.get().scheduler().runTimer(this::tickAll, 40L, 40L);
    }

    public LivingEntity summon(String id, Location loc) {
        MobDef def = plugin.registry().get(id);
        if (def == null || !def.boss) return null;

        var ticker = new kr.reborn.mob.spawn.SpawnTicker(plugin);
        LivingEntity e = ticker.spawnAt(def, loc);
        if (e == null) return null;
        e.setCustomName("§5§l[BOSS] §f" + def.name);

        BossBar bar = Bukkit.createBossBar(def.name, BarColor.PURPLE, BarStyle.SEGMENTED_10);
        bar.setProgress(1.0);
        int radius = plugin.getConfig().getInt("boss-announce-radius", 50);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld() == loc.getWorld() && p.getLocation().distance(loc) < radius) {
                bar.addPlayer(p);
                p.sendTitle("§5보스 출현!", "§f" + def.name, 10, 60, 20);
            }
        }
        List<BossPhase> phases = patternEngine.defaultPhases(def);
        BossInstance bi = new BossInstance(def, e, bar, phases);
        active.put(e.getUniqueId(), bi);
        // 진입 시 첫 페이즈 패턴 1발
        bi.enterPhase(0, patternEngine);
        return e;
    }

    public void onDamage(LivingEntity entity, Player player, double amount) {
        BossInstance b = active.get(entity.getUniqueId());
        if (b == null) return;
        b.contribute(player.getUniqueId(), amount);
        double frac = Math.max(0, entity.getHealth() / entity.getMaxHealth());
        b.bar.setProgress(frac);
        // 페이즈 전환 체크
        b.checkPhaseTransition(frac * 100, patternEngine);
    }

    public void onDeath(LivingEntity entity) {
        BossInstance b = active.remove(entity.getUniqueId());
        if (b == null) return;
        b.bar.removeAll();
        Bukkit.broadcastMessage("§6[보스 처치] §f" + b.def.name);
        // 기여도 상위 N명 보상
        int top = plugin.getConfig().getInt("boss-contribution-top", 10);
        b.contributions.entrySet().stream()
                .sorted((a, c) -> Double.compare(c.getValue(), a.getValue()))
                .limit(top)
                .forEach(e -> {
                    Player p = Bukkit.getPlayer(e.getKey());
                    if (p != null) {
                        p.sendMessage("§6보스 기여 보상 지급");
                        // 드롭 미니 보상 — 첫번째 드롭 1개 보장 (간단화)
                        for (var d : b.def.drops) {
                            try {
                                var mat = org.bukkit.Material.matchMaterial(d.item);
                                if (mat != null) {
                                    p.getInventory().addItem(
                                            new org.bukkit.inventory.ItemStack(mat, Math.max(1, d.min)));
                                    break;
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                });
    }

    public void tickAll() {
        long now = System.currentTimeMillis();
        for (BossInstance b : active.values()) {
            if (b.entity.isDead() || !b.entity.isValid()) continue;
            b.tickPatterns(now, patternEngine);
        }
    }

    public BossInstance instance(UUID entityId) { return active.get(entityId); }

    /** 받는 데미지 모디파이어 — listener가 데미지 이벤트 가공에 사용. */
    public double damageTakenMultiplier(UUID entityId) {
        BossInstance b = active.get(entityId);
        if (b == null || b.currentPhase < 0 || b.currentPhase >= b.phases.size()) return 1.0;
        return b.phases.get(b.currentPhase).damageTakenMultiplier;
    }

    public double damageMultiplier(UUID entityId) {
        BossInstance b = active.get(entityId);
        if (b == null || b.currentPhase < 0 || b.currentPhase >= b.phases.size()) return 1.0;
        return b.phases.get(b.currentPhase).damageMultiplier;
    }

    public static final class BossInstance {
        public final MobDef def;
        public final LivingEntity entity;
        public final BossBar bar;
        public final List<BossPhase> phases;
        public int currentPhase = -1;
        public final Map<UUID, Double> contributions = new HashMap<>();
        public final Map<BossPattern, Long> nextPatternAt = new HashMap<>();

        BossInstance(MobDef def, LivingEntity e, BossBar bar, List<BossPhase> phases) {
            this.def = def; this.entity = e; this.bar = bar; this.phases = phases;
        }

        void contribute(UUID id, double v) { contributions.merge(id, v, Double::sum); }

        public void enterPhase(int idx, PatternEngine engine) {
            if (idx < 0 || idx >= phases.size()) return;
            currentPhase = idx;
            BossPhase ph = phases.get(idx);
            Bukkit.broadcastMessage("§5§l[" + def.name + "] §c페이즈 " + ph.index + " — " + ph.label);
            bar.setColor(idx == 0 ? BarColor.PURPLE
                    : idx == 1 ? BarColor.YELLOW : BarColor.RED);
            // 페이즈 진입 즉시 첫 패턴 1발
            if (!ph.patterns.isEmpty()) {
                engine.trigger(entity, def, ph.patterns.get(0));
                nextPatternAt.put(ph.patterns.get(0),
                        System.currentTimeMillis() + ph.patterns.get(0).cooldownMs);
            }
        }

        public void checkPhaseTransition(double hpPercent, PatternEngine engine) {
            for (int i = phases.size() - 1; i > currentPhase; i--) {
                if (hpPercent <= phases.get(i).enterHpPercent) {
                    enterPhase(i, engine);
                    return;
                }
            }
        }

        public void tickPatterns(long now, PatternEngine engine) {
            if (currentPhase < 0 || currentPhase >= phases.size()) return;
            BossPhase ph = phases.get(currentPhase);
            for (BossPattern pat : ph.patterns) {
                long next = nextPatternAt.getOrDefault(pat, 0L);
                if (now >= next) {
                    engine.trigger(entity, def, pat);
                    nextPatternAt.put(pat, now + pat.cooldownMs);
                }
            }
        }
    }
}
