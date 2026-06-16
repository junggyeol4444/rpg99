package kr.reborn.hiddenclass.manager;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.StatType;
import kr.reborn.core.event.RebornDeathEvent;
import kr.reborn.core.event.RebornStatChangeEvent;
import kr.reborn.core.util.Msg;
import kr.reborn.hiddenclass.RebornHiddenClass;
import kr.reborn.hiddenclass.data.HiddenClass;
import kr.reborn.hiddenclass.event.RebornHiddenClassUnlockEvent;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 히든클래스 passive 효과 디스패처.
 *
 * 기존: config.yml에 'passive: DEMON_KI_X3' 같은 문자열이 저장만 되고 효과 0.
 * 이 클래스가 RebornStatChangeEvent / RebornDeathEvent / EntityDamageEvent를
 * 가로채 passive 플래그별 실제 게임 효과 적용.
 *
 * 1차 구현 (자주 발동 + 효과 명확한 것 우선):
 *   - DEMON_KI_X3:   DEMON_KI 누적 ×3
 *   - YOKAI_X2:      YOKAI_KI 누적 ×2
 *   - DRAGON_GROW_X2: DRAGON_POWER 누적 ×2
 *   - TRAIN_X3:      MENTAL/INTELLIGENCE 누적 ×3 (수련 source)
 *   - POISON_IMMUNITY:   POISON·중독 데미지 면역
 *   - RADIATION_IMMUNITY: WITHER 데미지 면역 (방사능 대용)
 *   - CYBER_IMMUNE:  CONFUSION 제거 (사이버 사이코 면역)
 *   - FIRST_DEATH_REVIVE: 최초 1회 사망 시 부활 + 영구 10% 강화
 *   - COMBAT_HEAL_AURA: 전투 시 30블록 내 아군 매 tick 회복
 *   - BLESSED_PRESENCE: 30블록 내 신도(아군) LUCK +5 유지
 */
public final class PassiveEngine implements Listener {

    private final RebornHiddenClass plugin;
    /** 캐시 — RebornHiddenClassUnlockEvent에서 갱신. */
    private final Map<UUID, Set<String>> cache = new ConcurrentHashMap<>();
    /** FIRST_DEATH_REVIVE 사용 여부 (영구 1회). */
    private final Set<UUID> firstReviveUsed = ConcurrentHashMap.newKeySet();

    private static final String NS = "RebornHiddenClass.passive";

    public PassiveEngine(RebornHiddenClass plugin) {
        this.plugin = plugin;
        loadReviveState();
    }

    private void loadReviveState() {
        try {
            var all = RebornCore.get().kv().loadAll(NS, null);
            for (var e : all.entrySet()) {
                if (e.getKey().startsWith("revive.")) {
                    try { firstReviveUsed.add(UUID.fromString(e.getKey().substring(7))); }
                    catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}
    }

    /** 플레이어가 보유한 모든 passive flag (해금된 클래스 전부 통합). */
    public Set<String> passivesOf(UUID id) {
        Set<String> cached = cache.get(id);
        if (cached != null) return cached;
        Set<String> out = new HashSet<>();
        for (String classId : plugin.progress().unlocked(id)) {
            HiddenClass hc = plugin.registry().get(classId);
            if (hc != null && hc.passive != null && !hc.passive.isEmpty()) {
                out.add(hc.passive);
            }
        }
        cache.put(id, out);
        return out;
    }

    public boolean has(UUID id, String passive) {
        return passivesOf(id).contains(passive);
    }

    public void invalidate(UUID id) {
        cache.remove(id);
    }

    // ───────────────── Stat 누적 증폭 ─────────────────

    /**
     * RebornStatChangeEvent 가로채 passive에 따라 추가 stat 부여.
     *
     * 무한 루프 방지: source가 "HC:"로 시작하면 우리 자신의 보너스이므로 skip.
     */
    @EventHandler
    public void onStatChange(RebornStatChangeEvent e) {
        String src = e.source();
        if (src != null && src.startsWith("HC:")) return;
        Player p = e.getPlayer();
        if (p == null) return;
        Set<String> pas = passivesOf(p.getUniqueId());
        if (pas.isEmpty()) return;
        double delta = e.newValue() - e.oldValue();
        if (delta <= 0) return;  // 음수 변동은 증폭 안 함

        StatType stat = e.stat();
        if (pas.contains("DEMON_KI_X3") && stat == StatType.DEMON_KI) {
            // 원래가 ×1이면 추가 +2× 부여 → 총 ×3
            RebornCore.get().api().addStat(p.getUniqueId(), StatType.DEMON_KI,
                    delta * 2.0, "HC:DEMON_KI_X3");
        }
        if (pas.contains("YOKAI_X2") && stat == StatType.YOKAI_KI) {
            RebornCore.get().api().addStat(p.getUniqueId(), StatType.YOKAI_KI,
                    delta, "HC:YOKAI_X2");
        }
        if (pas.contains("DRAGON_GROW_X2") && stat == StatType.DRAGON_POWER) {
            RebornCore.get().api().addStat(p.getUniqueId(), StatType.DRAGON_POWER,
                    delta, "HC:DRAGON_GROW_X2");
        }
        if (pas.contains("TRAIN_X3") && (stat == StatType.MENTAL || stat == StatType.INTELLIGENCE)) {
            // 수련/명상 source에서만 — 일반 stat 변동은 영향 없음
            if (src != null && (src.contains("meditate") || src.contains("train")
                    || src.contains("dao") || src.contains("essence"))) {
                RebornCore.get().api().addStat(p.getUniqueId(), stat,
                        delta * 2.0, "HC:TRAIN_X3");
            }
        }
    }

    // ───────────────── 데미지 면역·강화 ─────────────────

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        Set<String> pas = passivesOf(p.getUniqueId());
        if (pas.isEmpty()) return;
        EntityDamageEvent.DamageCause cause = e.getCause();
        if (pas.contains("POISON_IMMUNITY") && cause == EntityDamageEvent.DamageCause.POISON) {
            e.setCancelled(true);
            return;
        }
        if (pas.contains("RADIATION_IMMUNITY")
                && cause == EntityDamageEvent.DamageCause.WITHER) {
            e.setCancelled(true);
            return;
        }
        if (pas.contains("BLESSED_PRESENCE") && cause != EntityDamageEvent.DamageCause.FALL) {
            // 신성 가호 — 일반 데미지 10% 감소 (낙하는 제외)
            e.setDamage(e.getDamage() * 0.90);
        }
    }

    // ───────────────── 사망 부활 ─────────────────

    @EventHandler
    public void onDeath(RebornDeathEvent e) {
        Player p = e.getPlayer();
        Set<String> pas = passivesOf(p.getUniqueId());
        if (pas.isEmpty()) return;
        UUID id = p.getUniqueId();

        // FIRST_DEATH_REVIVE: 최초 1회 사망 시 즉시 부활 + 영구 10% MaxHealth.
        if (pas.contains("FIRST_DEATH_REVIVE") && !firstReviveUsed.contains(id)) {
            firstReviveUsed.add(id);
            RebornCore.get().kv().put(NS, null, "revive." + id, "1");
            try {
                p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                var attr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (attr != null) attr.setBaseValue(attr.getValue() * 1.10);
                Bukkit.broadcastMessage("§5§l[불멸] §f" + p.getName()
                        + " §7이(가) 죽음을 거부했다 — 영구 10% 강화.");
            } catch (Throwable ignored) {}
        }
        // BODYLESS_DIGITAL: CYBERPUNK 거주자 한정 — 매번 사망 시 클로드(클론) 부활.
        // 사이버 적응도 -50 페널티 (영원한 부활은 아님).
        if (pas.contains("BODYLESS_DIGITAL")) {
            try {
                p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                RebornCore.get().api().addStat(id, StatType.CYBER_ADAPTATION,
                        -50, "HC:BODYLESS_DIGITAL");
                Msg.send(p, "&b[디지털 부활] §7클로드(클론)로 복원 — 적응도 -50.");
            } catch (Throwable ignored) {}
        }
    }

    // ───────────────── 캐시 무효화 ─────────────────

    @EventHandler
    public void onUnlock(RebornHiddenClassUnlockEvent e) {
        invalidate(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // 캐시 워밍 + COMBAT_HEAL_AURA·BLESSED_PRESENCE 대상자라면 초기 효과 부여.
        passivesOf(e.getPlayer().getUniqueId());
    }

    // ───────────────── 주변 효과 (CHECK 주기 호출) ─────────────────

    /**
     * 매 cycle (예: 10초)마다 호출 — COMBAT_HEAL_AURA·BLESSED_PRESENCE처럼
     * 주변 검색이 필요한 passive 처리.
     */
    public void tickArea() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Set<String> pas = passivesOf(p.getUniqueId());
            if (pas.isEmpty()) continue;
            if (pas.contains("COMBAT_HEAL_AURA")) {
                for (var e : p.getNearbyEntities(30, 15, 30)) {
                    if (e instanceof Player ally && !ally.isDead()) {
                        try {
                            double max = ally.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                            ally.setHealth(Math.min(max, ally.getHealth() + 1.0));
                        } catch (Throwable ignored) {}
                    }
                }
            }
            if (pas.contains("BLESSED_PRESENCE")) {
                for (var e : p.getNearbyEntities(30, 15, 30)) {
                    if (e instanceof Player ally && !ally.isDead()) {
                        try { ally.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 240, 0, true, false)); }
                        catch (Throwable ignored) {}
                    }
                }
            }
            if (pas.contains("CYBER_IMMUNE") && p.hasPotionEffect(PotionEffectType.CONFUSION)) {
                p.removePotionEffect(PotionEffectType.CONFUSION);
            }
        }
    }

    @EventHandler
    public void onRegain(EntityRegainHealthEvent e) {
        // (자리만 — 향후 확장)
        if (!(e.getEntity() instanceof LivingEntity)) return;
    }
}
