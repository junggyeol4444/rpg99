package kr.reborn.hiddenclass.ability;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.StatType;
import kr.reborn.core.util.Msg;
import kr.reborn.hiddenclass.RebornHiddenClass;
import kr.reborn.hiddenclass.data.HiddenClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 히든 클래스 능력 엔진.
 *
 * - 보유 클래스의 능력만 발동 가능 (HiddenAbility.classId == HiddenClass.name 매칭).
 * - PASSIVE: tickPassives()가 10초 주기로 보유자에게 자동 효과 적용.
 * - ACTIVE: cast()로 발동, cooldownMs 후 재사용 가능 (-1=영구, 0=1회한정).
 * - 일부 능력은 reflection으로 RebornSkill/RebornNPC와 협업.
 */
public final class AbilityEngine {

    private final RebornHiddenClass plugin;
    /** uuid → abilityName → 다음 사용 가능 시각(ms). */
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    /** 1회 한정 능력 사용 기록. */
    private final Map<UUID, java.util.Set<String>> oneShotUsed = new ConcurrentHashMap<>();
    /** IMMORTAL_REVIVE 발동 여부 (1회 한정 → 부활 후 영구 강화 적용 표시). */
    private final Map<UUID, Boolean> immortalConsumed = new ConcurrentHashMap<>();

    public AbilityEngine(RebornHiddenClass plugin) {
        this.plugin = plugin;
        // 10초마다 passive tick
        RebornCore.get().scheduler().runTimer(this::tickPassives, 200L, 200L);
    }

    /* ───────────────────────────── 보유 검사 ───────────────────────────── */

    public boolean owns(Player p, HiddenAbility ab) {
        for (String unlockedId : plugin.progress().unlocked(p.getUniqueId())) {
            HiddenClass hc = plugin.registry().get(unlockedId);
            if (hc != null && plain(hc.name).equals(ab.classId)) return true;
        }
        return false;
    }

    /** 컬러코드 제거 후 비교용. */
    private String plain(String s) {
        if (s == null) return "";
        // §a, &b 등 모두 제거
        return s.replaceAll("(?i)[§&][0-9A-FK-OR]", "").trim();
    }

    /* ───────────────────────────── ACTIVE ───────────────────────────── */

    public boolean cast(Player p, HiddenAbility ab, String targetName) {
        if (ab.passive) { Msg.warn(p, "패시브 능력 — 자동 적용."); return false; }
        if (!owns(p, ab)) { Msg.error(p, "보유하지 않은 능력: " + ab.classId); return false; }
        UUID id = p.getUniqueId();

        if (ab.cooldownMs == 0) {
            var used = oneShotUsed.computeIfAbsent(id, k -> new java.util.HashSet<>());
            if (used.contains(ab.name())) { Msg.error(p, "1회 한정 능력 — 이미 사용함."); return false; }
            used.add(ab.name());
        } else if (ab.cooldownMs > 0) {
            long now = System.currentTimeMillis();
            long ready = cooldowns.computeIfAbsent(id, k -> new HashMap<>()).getOrDefault(ab.name(), 0L);
            if (now < ready) {
                Msg.warn(p, "쿨다운 " + ((ready - now) / 1000) + "초 남음.");
                return false;
            }
            cooldowns.get(id).put(ab.name(), now + ab.cooldownMs);
        }
        // -1 = 영구 패시브이지만 ACTIVE로 분류된 능력은 즉발/토글 (CD 없음).
        apply(p, ab, targetName);
        return true;
    }

    private void apply(Player p, HiddenAbility ab, String targetName) {
        Bukkit.broadcastMessage(Msg.PREFIX + Msg.c("&6&l[히든능력] &f" + p.getName()
                + " &7→ &e" + ab.classId + "&7 발동!"));
        try { p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.5f); } catch (Throwable ignored) {}
        switch (ab) {
            case CHAOS_BURST -> chaosBurst(p);
            case DEMON_KI_OVERFLOW -> demonKiOverflow(p);
            case SPIRIT_SIGHT -> spiritSight(p);
            case TRANSFORM_ROAR -> transformRoar(p);
            case DUAL_CAST -> dualCast(p, targetName);
            case CULT_COMMAND -> cultCommand(p);
            case FORBIDDEN_POWER -> forbiddenPower(p);
            case YOKAI_EMPEROR -> yokaiEmperor(p);
            case DRAGON_LORD_ROAR -> dragonLordRoar(p);
            case APOCALYPSE_COMMAND -> apocalypseCommand(p);
            case ABYSS_TOUCH -> abyssTouch(p, targetName);
            case SOUL_TRAVEL -> soulTravel(p);
            case TIME_REWIND -> timeRewind(p);
            case DREAM_INTRUSION -> dreamIntrusion(p, targetName);
            case LABYRINTH_TELEPORT -> labyrinthTeleport(p);
            case AI_COMMAND -> aiCommand(p);
            case SEA_KING_COMMAND -> seaKingCommand(p);
            case PRIMORDIAL_CONNECT -> primordialConnect(p);
            default -> Msg.warn(p, "이 능력은 단일 발동 효과가 없다 — 패시브 또는 트리거형.");
        }
    }

    /* ─────────────── 각 능력 구현 ─────────────── */

    private void chaosBurst(Player p) {
        Location l = p.getLocation();
        try { p.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, l, 5); } catch (Throwable ignored) {}
        try { p.getWorld().spawnParticle(Particle.FLAME, l, 200, 5, 2, 5, 0.2); } catch (Throwable ignored) {}
        try { p.getWorld().spawnParticle(Particle.SPELL_WITCH, l, 200, 5, 2, 5, 0.2); } catch (Throwable ignored) {}
        for (Entity e : p.getNearbyEntities(8, 8, 8)) {
            if (e instanceof LivingEntity le && !(e instanceof Player)) {
                le.damage(80.0, p);
                try { le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1)); } catch (Throwable ignored) {}
            }
        }
    }

    private void demonKiOverflow(Player p) {
        try {
            p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 600, 4));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 600, 2));
            p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 600, 0));
        } catch (Throwable ignored) {}
        try { p.getWorld().spawnParticle(Particle.DRAGON_BREATH, p.getLocation(), 80, 3, 1, 3); } catch (Throwable ignored) {}
    }

    private void spiritSight(Player p) {
        try {
            p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 1200, 0));
            p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 1200, 0));
        } catch (Throwable ignored) {}
        // 주변 적에게 1200tick GLOWING — 약점 시각화
        for (Entity e : p.getNearbyEntities(30, 30, 30)) {
            if (e instanceof LivingEntity le && !(e instanceof Player)) {
                try { le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 1200, 0)); } catch (Throwable ignored) {}
            }
        }
    }

    private void transformRoar(Player p) {
        try {
            p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 1200, 2));
            p.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 1200, 4));
            p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + 10));
        } catch (Throwable ignored) {}
        for (Entity e : p.getNearbyEntities(15, 15, 15)) {
            if (e instanceof LivingEntity le && !(e instanceof Player)) {
                try {
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 200, 2));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 200, 0));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 1));
                } catch (Throwable ignored) {}
            }
        }
    }

    private void dualCast(Player p, String targetName) {
        // RebornSkill API를 통해 등록된 2개 스킬을 동시에 시전
        try {
            var sp = Bukkit.getPluginManager().getPlugin("RebornSkill");
            if (sp == null) { Msg.warn(p, "RebornSkill 없음 — 효과 축소."); damageInFront(p, 50); return; }
            // 마법(첫번째 학습 마법) + 검술(첫번째 학습 검) 둘다 발사
            sp.getClass().getMethod("castFirstMagic", Player.class).invoke(sp, p);
            sp.getClass().getMethod("castFirstSword", Player.class).invoke(sp, p);
        } catch (Throwable t) { damageInFront(p, 70); }
    }

    private void cultCommand(Player p) {
        // 반경 30 내 NPC를 30초간 직속화 — RebornNPC reflection
        try {
            var np = Bukkit.getPluginManager().getPlugin("RebornNPC");
            if (np != null) {
                np.getClass().getMethod("commandNearbyNpcs", Player.class, double.class, long.class, String.class)
                        .invoke(np, p, 30.0, 30_000L, "DEMON");
            }
        } catch (Throwable ignored) {}
        try { p.getWorld().spawnParticle(Particle.SPELL_MOB, p.getLocation(), 200, 10, 3, 10, 0.0); } catch (Throwable ignored) {}
    }

    private void forbiddenPower(Player p) {
        try {
            p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 1200, 5));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 3));
            p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 1200, 2));
            // 폭주 후 페널티 (10초간 WITHER)
            p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 1400, 0));
        } catch (Throwable ignored) {}
    }

    private void yokaiEmperor(Player p) {
        try {
            var np = Bukkit.getPluginManager().getPlugin("RebornNPC");
            if (np != null) {
                np.getClass().getMethod("commandNearbyNpcs", Player.class, double.class, long.class, String.class)
                        .invoke(np, p, 40.0, 120_000L, "YOKAI");
            }
        } catch (Throwable ignored) {}
        for (Entity e : p.getNearbyEntities(40, 40, 40)) {
            if (e instanceof LivingEntity le && !(e instanceof Player)) {
                try { le.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 400, 2)); } catch (Throwable ignored) {}
            }
        }
    }

    private void dragonLordRoar(Player p) {
        try {
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);
            p.getWorld().spawnParticle(Particle.DRAGON_BREATH, p.getLocation(), 500, 15, 5, 15);
        } catch (Throwable ignored) {}
        for (Entity e : p.getNearbyEntities(30, 30, 30)) {
            if (e instanceof LivingEntity le && !(e instanceof Player)) {
                try {
                    // 기절 = 강한 SLOW + JUMP 음수 + BLINDNESS 5초
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 6));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 2));
                } catch (Throwable ignored) {}
            }
        }
        try {
            p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 200, 3));
        } catch (Throwable ignored) {}
    }

    private void apocalypseCommand(Player p) {
        for (Entity e : p.getNearbyEntities(50, 50, 50)) {
            if (e instanceof LivingEntity le && !(e instanceof Player)) {
                try {
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 600, 3));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 600, 1));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 600, 1));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 600, 2));
                } catch (Throwable ignored) {}
            }
        }
        try { p.getWorld().spawnParticle(Particle.SMOKE_LARGE, p.getLocation(), 400, 20, 5, 20); } catch (Throwable ignored) {}
    }

    private void abyssTouch(Player p, String targetName) {
        LivingEntity target = resolveTarget(p, targetName);
        if (target == null) { Msg.error(p, "대상 없음 — 가장 가까운 적 자동 선택 실패."); return; }
        try {
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 80, 4));
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 80, 6));
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 80, 0));
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 9));
            target.damage(40, p);
            target.getWorld().spawnParticle(Particle.SQUID_INK, target.getLocation(), 100, 1, 1, 1);
        } catch (Throwable ignored) {}
    }

    private void soulTravel(Player p) {
        // 시선이 향한 곳으로 즉시 텔레포트 (최대 80블록)
        try {
            org.bukkit.block.Block tgt = p.getTargetBlockExact(80);
            if (tgt != null) {
                Location dest = tgt.getLocation().add(0.5, 1.1, 0.5);
                dest.setYaw(p.getLocation().getYaw());
                dest.setPitch(p.getLocation().getPitch());
                p.teleport(dest);
                p.getWorld().spawnParticle(Particle.PORTAL, dest, 100, 0.5, 1.0, 0.5);
            } else Msg.warn(p, "시선이 빈 공간 — 텔레포트 실패.");
        } catch (Throwable ignored) {}
    }

    /** 5초 전 위치·체력 저장 + 되돌리기. */
    private final Map<UUID, Location> rewindLoc = new ConcurrentHashMap<>();
    private final Map<UUID, Double> rewindHp = new ConcurrentHashMap<>();
    private final Map<UUID, Long> rewindTime = new ConcurrentHashMap<>();

    /** PlayerMoveEvent에서 매 tick 호출 가능 — 5초 이상 경과한 기록은 갱신. */
    public void snapshotRewind(Player p) {
        UUID id = p.getUniqueId();
        long last = rewindTime.getOrDefault(id, 0L);
        if (System.currentTimeMillis() - last < 5000) return;
        rewindLoc.put(id, p.getLocation().clone());
        rewindHp.put(id, p.getHealth());
        rewindTime.put(id, System.currentTimeMillis());
    }

    private void timeRewind(Player p) {
        Location l = rewindLoc.get(p.getUniqueId());
        if (l == null) { Msg.warn(p, "기록된 위치 없음 — 5초 후 재시도."); return; }
        try {
            p.teleport(l);
            p.setHealth(Math.min(p.getMaxHealth(), rewindHp.getOrDefault(p.getUniqueId(), p.getHealth())));
            p.getWorld().spawnParticle(Particle.END_ROD, l, 50, 1, 1, 1);
        } catch (Throwable ignored) {}
    }

    private void dreamIntrusion(Player p, String targetName) {
        LivingEntity target = resolveTarget(p, targetName);
        if (target == null) { Msg.error(p, "대상 없음."); return; }
        try {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 9));
            target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 100, 128));
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 9));
            target.getWorld().spawnParticle(Particle.SPELL_WITCH, target.getLocation(), 60, 1, 1, 1);
        } catch (Throwable ignored) {}
    }

    private void labyrinthTeleport(Player p) {
        // 임의 층 = 랜덤 Y -50~200 + 같은 월드 안 거리 100~500 무작위 이동
        try {
            double dx = (Math.random() - 0.5) * 1000;
            double dz = (Math.random() - 0.5) * 1000;
            double y = -50 + Math.random() * 250;
            Location dest = p.getLocation().add(dx, 0, dz);
            dest.setY(Math.max(0, y));
            p.teleport(dest);
            p.getWorld().spawnParticle(Particle.PORTAL, dest, 200, 1, 2, 1);
        } catch (Throwable ignored) {}
    }

    private void aiCommand(Player p) {
        // 반경 50 내 IRON_GOLEM, 사이버 골렘 등을 우호화
        for (Entity e : p.getNearbyEntities(50, 50, 50)) {
            if (e instanceof org.bukkit.entity.IronGolem g) {
                try { g.setPlayerCreated(true); } catch (Throwable ignored) {}
            }
        }
        try {
            var np = Bukkit.getPluginManager().getPlugin("RebornNPC");
            if (np != null) {
                np.getClass().getMethod("commandNearbyNpcs", Player.class, double.class, long.class, String.class)
                        .invoke(np, p, 50.0, 300_000L, "CYBER");
            }
        } catch (Throwable ignored) {}
    }

    private void seaKingCommand(Player p) {
        for (Entity e : p.getNearbyEntities(50, 50, 50)) {
            if (e instanceof org.bukkit.entity.WaterMob wm) {
                try { wm.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 1200, 1)); } catch (Throwable ignored) {}
            }
        }
        try {
            p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 6000, 0));
            p.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 6000, 1));
            p.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, 6000, 0));
        } catch (Throwable ignored) {}
    }

    private void primordialConnect(Player p) {
        // 빛/어둠 동시 발현 — 자신 강화 + 적에게 BLINDNESS·GLOWING
        try {
            p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 1200, 4));
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 1200, 2));
            p.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 1200, 9));
            p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation(), 300, 5, 5, 5);
        } catch (Throwable ignored) {}
        for (Entity e : p.getNearbyEntities(30, 30, 30)) {
            if (e instanceof LivingEntity le && !(e instanceof Player)) {
                try {
                    le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 0));
                    le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 1200, 0));
                } catch (Throwable ignored) {}
            }
        }
    }

    /* ───────────────────────────── PASSIVE TICK ───────────────────────────── */

    public void tickPassives() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            // Folia-safe: 각 플레이어의 entity 스케줄러에 위임
            RebornCore.get().scheduler().runRegionTask(p.getLocation(), () -> {
                for (String unlockedId : plugin.progress().unlocked(p.getUniqueId())) {
                    HiddenClass hc = plugin.registry().get(unlockedId);
                    if (hc == null) continue;
                    for (HiddenAbility ab : HiddenAbility.values()) {
                        if (!ab.passive) continue;
                        if (!plain(hc.name).equals(ab.classId)) continue;
                        try { tickPassive(p, ab); } catch (Throwable ignored) {}
                    }
                }
            });
        }
    }

    private void tickPassive(Player p, HiddenAbility ab) {
        switch (ab) {
            case DIVINE_FAVOR -> {
                try {
                    RebornCore.get().api().addStat(p.getUniqueId(), StatType.LUCK, 0.05, "HC:DIVINE_FAVOR");
                    RebornCore.get().api().addStat(p.getUniqueId(), StatType.DIVINITY, 0.01, "HC:DIVINE_FAVOR");
                } catch (Throwable ignored) {}
            }
            case IMMORTAL_REVIVE -> {
                // 부활 후 영구 강화 표시만 — 실제 부활 트리거는 EntityDamageEvent listener에서.
                if (Boolean.TRUE.equals(immortalConsumed.get(p.getUniqueId()))) {
                    try { p.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, 300, 0, true, false)); }
                    catch (Throwable ignored) {}
                }
            }
            case DRAGON_BREATH_AURA -> {
                try {
                    p.setAllowFlight(true);
                    if (p.getFireTicks() < 0) p.setFireTicks(0);
                } catch (Throwable ignored) {}
            }
            case NAMELESS_STEALTH -> {
                try {
                    // 은신 보너스 = INVISIBILITY 짧게 (10초 갱신)
                    p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 220, 0, true, false));
                } catch (Throwable ignored) {}
            }
            case GATE_RESONANCE -> {
                // 게이트 월드 안일 때만 +20% (= 효과 부여)
                String w = p.getWorld().getName().toLowerCase();
                if (w.contains("gate") || w.contains("earth")) {
                    try {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 220, 0, true, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 220, 0, true, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 220, 0, true, false));
                    } catch (Throwable ignored) {}
                }
            }
            case CYBER_IMMUNE -> {
                // 사이버 사이코 면역 = CONFUSION/POISON 즉시 제거
                try {
                    if (p.hasPotionEffect(PotionEffectType.CONFUSION)) p.removePotionEffect(PotionEffectType.CONFUSION);
                    if (p.hasPotionEffect(PotionEffectType.POISON)) p.removePotionEffect(PotionEffectType.POISON);
                } catch (Throwable ignored) {}
            }
            case DEMON_LORD_AURA -> {
                for (Entity e : p.getNearbyEntities(20, 20, 20)) {
                    if (e instanceof LivingEntity le && !(e instanceof Player)) {
                        try {
                            le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 220, 0, true, false));
                            le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 220, 0, true, false));
                        } catch (Throwable ignored) {}
                    }
                }
            }
            case DUNGEON_DECREE -> {
                // 던전 월드에서 패시브 행운/스폰 우호화 (월드 이름 검사)
                String w = p.getWorld().getName().toLowerCase();
                if (w.contains("dungeon") || w.contains("labyrinth")) {
                    try { p.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 220, 4, true, false)); }
                    catch (Throwable ignored) {}
                }
            }
            case HERO_AURA -> {
                // 주변 NPC 호의도 미세 누적
                try {
                    var np = Bukkit.getPluginManager().getPlugin("RebornNPC");
                    if (np != null) {
                        np.getClass().getMethod("nudgeNearbyFavor", Player.class, double.class, double.class)
                                .invoke(np, p, 30.0, 1.0);
                    }
                } catch (Throwable ignored) {}
                try {
                    RebornCore.get().api().addStat(p.getUniqueId(), StatType.DIVINITY, 0.005, "HC:HERO_AURA");
                    RebornCore.get().api().addStat(p.getUniqueId(), StatType.CHARISMA, 0.01, "HC:HERO_AURA");
                } catch (Throwable ignored) {}
            }
            case GENESIS_BLESSING -> {
                // 창세신기 보유 시 모든 스탯 미세 누적
                try {
                    for (StatType t : StatType.COMMON_8) {
                        RebornCore.get().api().addStat(p.getUniqueId(), t, 0.02, "HC:GENESIS_BLESSING");
                    }
                } catch (Throwable ignored) {}
            }
            case MASTER_OF_TRADES -> {
                try { p.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 220, 1, true, false)); }
                catch (Throwable ignored) {}
            }
            case PIRATE_KING_FLAG -> {
                try {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 220, 0, true, false));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 220, 0, true, false));
                } catch (Throwable ignored) {}
            }
            case DREAM_LORD_PASSIVE -> {
                // 식사 게이지 낮으면 = 수면 모드로 간주 → 수련 효과
                if (p.getFoodLevel() < 10) {
                    try {
                        RebornCore.get().api().addStat(p.getUniqueId(), StatType.MENTAL, 0.05, "HC:DREAM_LORD");
                        RebornCore.get().api().addStat(p.getUniqueId(), StatType.INTELLIGENCE, 0.03, "HC:DREAM_LORD");
                    } catch (Throwable ignored) {}
                }
            }
            default -> { /* 기타 패시브는 unlock 시 statBonuses로 이미 적용 */ }
        }
    }

    /* ───────────────────────────── IMMORTAL_REVIVE 트리거 ───────────────────────────── */

    /** EntityDamageEvent listener에서 호출 — 사망 직전 발동 1회. */
    public boolean tryImmortalRevive(Player p) {
        UUID id = p.getUniqueId();
        // IMMORTAL_REVIVE 보유 확인
        boolean has = false;
        for (String unlockedId : plugin.progress().unlocked(id)) {
            HiddenClass hc = plugin.registry().get(unlockedId);
            if (hc != null && plain(hc.name).equals(HiddenAbility.IMMORTAL_REVIVE.classId)) { has = true; break; }
        }
        if (!has) return false;
        if (Boolean.TRUE.equals(immortalConsumed.get(id))) return false;
        immortalConsumed.put(id, true);
        try {
            p.setHealth(p.getMaxHealth());
            p.addPotionEffect(new PotionEffect(PotionEffectType.HEALTH_BOOST, Integer.MAX_VALUE, 1, true, false));
            // 영구 10% 강화 = COMMON 8 스탯 += 10% of current
            for (StatType t : StatType.COMMON_8) {
                double cur = RebornCore.get().api().getStat(id, t);
                RebornCore.get().api().addStat(id, t, cur * 0.10, "HC:IMMORTAL_REVIVE");
            }
            p.getWorld().spawnParticle(Particle.TOTEM, p.getLocation(), 200, 1, 1, 1);
            p.getWorld().playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 1.5f, 1.0f);
            Bukkit.broadcastMessage(Msg.PREFIX + Msg.c("&6&l[불멸자] &f" + p.getName() + " &7가 죽음을 거부했다!"));
        } catch (Throwable ignored) {}
        return true;
    }

    /* ───────────────────────────── helpers ───────────────────────────── */

    private LivingEntity resolveTarget(Player p, String name) {
        if (name != null && !name.isEmpty()) {
            Player t = Bukkit.getPlayerExact(name);
            if (t != null) return t;
        }
        LivingEntity nearest = null;
        double bestSq = 30 * 30;
        for (Entity e : p.getNearbyEntities(30, 30, 30)) {
            if (!(e instanceof LivingEntity le)) continue;
            if (e == p) continue;
            double d = e.getLocation().distanceSquared(p.getLocation());
            if (d < bestSq) { bestSq = d; nearest = le; }
        }
        return nearest;
    }

    private void damageInFront(Player p, double dmg) {
        Location l = p.getLocation();
        org.bukkit.util.Vector dir = l.getDirection().normalize().multiply(3);
        Location front = l.clone().add(dir);
        for (Entity e : p.getWorld().getNearbyEntities(front, 5, 3, 5)) {
            if (e instanceof LivingEntity le && e != p) le.damage(dmg, p);
        }
    }

    public long cooldownRemaining(UUID p, HiddenAbility ab) {
        Long ready = cooldowns.getOrDefault(p, Map.of()).get(ab.name());
        if (ready == null) return 0;
        return Math.max(0, ready - System.currentTimeMillis());
    }
}
