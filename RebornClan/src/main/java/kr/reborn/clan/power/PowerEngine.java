package kr.reborn.clan.power;

import kr.reborn.clan.RebornClan;
import kr.reborn.core.util.Msg;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 72귀족 가문 권능을 실제로 발동시키는 엔진.
 *
 * 권능 이름을 한국어 키워드로 카테고리화(공격AOE/속박/방패/지배오라/소환/독·저주/
 * 치유/탐지/분노)하여 Bukkit 효과로 매핑. 데이터로만 있던 권능이 명령으로 실행된다.
 * 쿨다운은 권능별·플레이어별 분리.
 */
public final class PowerEngine {

    private final RebornClan plugin;
    private final Set<String> knownPowers = new HashSet<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public PowerEngine(RebornClan plugin) { this.plugin = plugin; load(); }

    public void load() {
        knownPowers.clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("npc-clans");
        if (root == null) return;
        for (String worldKey : root.getKeys(false)) {
            ConfigurationSection world = root.getConfigurationSection(worldKey);
            if (world == null) continue;
            for (String houseId : world.getKeys(false)) {
                List<String> powers = world.getStringList(houseId + ".powers");
                knownPowers.addAll(powers);
            }
        }
        plugin.getLogger().info("권능 " + knownPowers.size() + "종 로드");
    }

    public Set<String> all() { return knownPowers; }
    public boolean isKnown(String name) { return knownPowers.contains(name); }

    public boolean use(Player p, String powerName) {
        if (!isKnown(powerName)) return false;
        long now = System.currentTimeMillis();
        long cd = plugin.getConfig().getLong("powers.cooldown-seconds", 60) * 1000L;
        Long next = cooldowns.computeIfAbsent(p.getUniqueId(), x -> new HashMap<>()).get(powerName);
        if (next != null && now < next) {
            Msg.warn(p, "쿨다운 " + (next - now) / 1000 + "초");
            return false;
        }
        cooldowns.get(p.getUniqueId()).put(powerName, now + cd);
        apply(p, powerName);
        return true;
    }

    private void apply(Player p, String powerName) {
        String n = powerName;
        // 권능 이름 해시 → 각 권능 고유 강도 변동 (±25%) — "전쟁의 불꽃"·"충성의 불꽃"이 같은 카테고리여도 강도가 다름
        int h = Math.abs(n.hashCode());
        double intensity = 0.75 + (h % 51) / 100.0;  // 0.75 ~ 1.25
        int rad = 5 + (h % 4);                       // 5 ~ 8 블록 범위 변동

        // 카테고리화 — 키워드 우선순위
        if (containsAny(n, "왕좌", "지배", "선언", "명령", "지휘", "왕의")) {
            auraStrength(p, (int)(8 * intensity), 240);
        } else if (containsAny(n, "전쟁의 불꽃", "충성의 불꽃")) {
            // 불꽃류는 화염 부가
            auraStrength(p, (int)(6 * intensity), 240);
            try { p.setFireTicks(0); } catch (Throwable ignored) {}
            for (var e : p.getNearbyEntities(rad, rad, rad)) {
                if (e instanceof LivingEntity le && !(e instanceof Player)) {
                    try { le.setFireTicks(60); } catch (Throwable ignored) {}
                }
            }
        } else if (containsAny(n, "소환", "군단", "헬하운드", "수하", "포탑", "심해 소환")) {
            summonMinions(p, n, 1 + (h % 3));
        } else if (containsAny(n, "사슬", "속박", "도주 불가", "감금", "꿈의 감옥", "정체의 시간")) {
            chainHold(p, rad, (int)(100 * intensity));
        } else if (containsAny(n, "방패", "보호", "방어", "보루", "장막")) {
            buffResistance(p, (int)(200 * intensity));
        } else if (containsAny(n, "은밀", "위장", "은신", "회피")) {
            // 은신 계열 — 투명화·속도
            try {
                p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, (int)(200 * intensity), 0));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int)(200 * intensity), 1));
            } catch (Throwable ignored) {}
        } else if (containsAny(n, "치유", "회복", "축복", "가호", "명예 회복")) {
            healSelf(p, 30 * intensity);
        } else if (containsAny(n, "치유 봉쇄")) {
            // 치유 봉쇄 — 적 회복 차단 (속도 디버프로 근사)
            aoeDot(p, rad, PotionEffectType.SLOW, 200);
            aoeDot(p, rad, PotionEffectType.WEAKNESS, 200);
        } else if (containsAny(n, "역병", "독", "부패", "감염", "쇠약")) {
            aoeDot(p, rad, PotionEffectType.POISON, (int)(100 * intensity));
        } else if (containsAny(n, "역공")) {
            // 역공 — 방어 + 광역 데미지
            buffResistance(p, 100);
            aoeDamage(p, rad, 10 * intensity, true);
        } else if (containsAny(n, "저주", "고통", "비탄", "원한")) {
            aoeDot(p, rad, PotionEffectType.WITHER, (int)(100 * intensity));
        } else if (containsAny(n, "탐지", "투시", "감지", "발견", "보물", "해부학", "투시안")) {
            utilityGlow(p, rad * 1.5, (int)(200 * intensity));
        } else if (containsAny(n, "분노", "광기", "광폭", "불의 재", "전소")) {
            auraStrength(p, (int)(12 * intensity), 200);
        } else if (containsAny(n, "지옥", "화염")) {
            aoeDamage(p, rad, 18 * intensity, true);
            for (var e : p.getNearbyEntities(rad, rad, rad)) {
                if (e instanceof LivingEntity le && !(e instanceof Player)) {
                    try { le.setFireTicks(80); } catch (Throwable ignored) {}
                }
            }
        } else if (containsAny(n, "벼락", "낙뢰")) {
            // 벼락 — 시각 효과 + 광역
            try { p.getWorld().strikeLightningEffect(p.getLocation()); } catch (Throwable ignored) {}
            aoeDamage(p, rad, 22 * intensity, false);
        } else if (containsAny(n, "지진")) {
            aoeDamage(p, rad + 2, 15 * intensity, true);
            // 적 점프 봉쇄
            for (var e : p.getNearbyEntities(rad, rad, rad)) {
                if (e instanceof LivingEntity le && !(e instanceof Player)) {
                    try { le.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 100, 128, true, false)); }
                    catch (Throwable ignored) {}
                }
            }
        } else if (containsAny(n, "해일", "대홍수", "분출")) {
            aoeDamage(p, rad + 1, 18 * intensity, true);
        } else if (containsAny(n, "심판", "처단")) {
            aoeDamage(p, rad + 2, 25 * intensity, false);
        } else if (containsAny(n, "폭발")) {
            aoeDamage(p, rad, 20 * intensity, true);
            try { p.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, p.getLocation(), 3); }
            catch (Throwable ignored) {}
        } else {
            // 기본: 자기 강화 + 광휘 (강도는 해시 기반으로 변동)
            buffResistance(p, (int)(200 * intensity));
            p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,
                    (int)(200 * intensity), 1));
        }
        p.getWorld().spawnParticle(Particle.SPELL_WITCH, p.getLocation().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.1);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1f);
        Msg.send(p, "&6&l[권능] &r&e" + powerName + " &7발동 §8(강도 "
                + String.format("%.2f", intensity) + ")");
    }

    // ───────────────────────── 효과 ─────────────────────────

    private void auraStrength(Player p, int amp, int dur) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, dur, Math.max(0, amp - 1)));
        for (Entity e : p.getNearbyEntities(8, 4, 8)) {
            if (e instanceof Player ally && ally != p && allyOf(p, ally)) {
                ally.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, dur, Math.max(0, amp - 2)));
            }
        }
    }

    private void buffResistance(Player p, int dur) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, dur, 2));
        p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, dur, 1));
    }

    private void chainHold(Player p, double range, int dur) {
        for (Entity e : p.getNearbyEntities(range, range, range)) {
            if (e instanceof LivingEntity le && !(e instanceof Player) && !le.isDead()) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, dur, 4));
                le.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, dur, 128));  // 음수 점프로 묶기
            }
        }
    }

    private void aoeDamage(Player p, double radius, double damage, boolean knockback) {
        if (p.getWorld() == null) return;
        p.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, p.getLocation(), 4);
        Vector center = p.getLocation().toVector();
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof LivingEntity le && !(e instanceof Player)) {
                le.damage(damage, p);
                if (knockback) {
                    Vector kb = le.getLocation().toVector().subtract(center).normalize().multiply(1.2).setY(0.4);
                    le.setVelocity(kb);
                }
            }
        }
    }

    private void aoeDot(Player p, double radius, PotionEffectType type, int dur) {
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof LivingEntity le && !(e instanceof Player)) {
                le.addPotionEffect(new PotionEffect(type, dur, 1));
            }
        }
    }

    private void healSelf(Player p, double amount) {
        double max = 20;
        var attr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) max = attr.getValue();
        p.setHealth(Math.min(max, p.getHealth() + amount));
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
    }

    private void utilityGlow(Player p, double range, int dur) {
        for (Entity e : p.getNearbyEntities(range, range, range)) {
            if (e instanceof LivingEntity le) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, dur, 0));
            }
        }
    }

    private void summonMinions(Player p, String powerName, int count) {
        if (p.getWorld() == null) return;
        EntityType type = pickMinionType(powerName);
        for (int i = 0; i < count; i++) {
            var at = p.getLocation().add(Math.random() * 2 - 1, 0, Math.random() * 2 - 1);
            Entity m = p.getWorld().spawnEntity(at, type);
            if (m instanceof LivingEntity ml) {
                ml.setCustomName("§b" + p.getName() + "의 수하");
                ml.setCustomNameVisible(true);
                if (m instanceof org.bukkit.entity.Tameable t) { t.setTamed(true); t.setOwner(p); }
            }
        }
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1f, 1f);
    }

    /** 권능 이름 키워드로 소환 종족 결정 — "헬하운드"는 WOLF, "심해 소환"은 GUARDIAN 등. */
    private EntityType pickMinionType(String n) {
        if (n.contains("헬하운드") || n.contains("늑대")) return EntityType.WOLF;
        if (n.contains("심해") || n.contains("바다") || n.contains("해양")) return EntityType.GUARDIAN;
        if (n.contains("포탑") || n.contains("골렘") || n.contains("기계")) return EntityType.IRON_GOLEM;
        if (n.contains("악마") || n.contains("마군") || n.contains("지옥")) return EntityType.WITHER_SKELETON;
        if (n.contains("천사") || n.contains("정령")) return EntityType.VEX;
        if (n.contains("언데드") || n.contains("죽음") || n.contains("강시")) return EntityType.ZOMBIE;
        if (n.contains("뱀") || n.contains("독")) return EntityType.SILVERFISH;
        if (n.contains("새") || n.contains("비행")) return EntityType.PHANTOM;
        // 기본: 군단
        return EntityType.PIGLIN;
    }

    private boolean allyOf(Player a, Player b) {
        var ca = plugin.clans().ofPlayer(a.getUniqueId());
        var cb = plugin.clans().ofPlayer(b.getUniqueId());
        return ca != null && cb != null && ca.id.equals(cb.id);
    }

    private boolean containsAny(String s, String... needles) {
        for (String n : needles) if (s.contains(n)) return true;
        return false;
    }
}
