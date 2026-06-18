package kr.reborn.curse.manager;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.StatType;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import kr.reborn.curse.RebornCurse;
import kr.reborn.curse.data.ActiveEffect;
import kr.reborn.curse.data.EffectDef;
import kr.reborn.curse.event.RebornBerserkEvent;
import kr.reborn.curse.event.RebornBlessingApplyEvent;
import kr.reborn.curse.event.RebornCurseApplyEvent;
import kr.reborn.curse.event.RebornCurseCureEvent;
import kr.reborn.curse.event.RebornCurseTickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 플레이어별 활성 효과 + tick 적용. */
public final class PlayerEffectManager {

    private static final String NS = "RebornCurse.active";

    private final RebornCurse plugin;
    private final Map<UUID, Map<String, ActiveEffect>> active = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> loaded = ConcurrentHashMap.newKeySet();

    public PlayerEffectManager(RebornCurse plugin) {
        this.plugin = plugin;
    }

    private void ensureLoaded(UUID p) {
        if (loaded.add(p)) {
            var all = RebornCore.get().kv().loadAll(NS, p);
            Map<String, ActiveEffect> map = new HashMap<>();
            for (var e : all.entrySet()) {
                try {
                    // "kind|stacks|remainingTicks|lastTickAt|berserkActive|berserkUntil"
                    String[] parts = e.getValue().split("\\|", -1);
                    if (parts.length < 4) continue;
                    EffectDef.Kind kind = EffectDef.Kind.valueOf(parts[0]);
                    int stacks = Integer.parseInt(parts[1]);
                    long remaining = Long.parseLong(parts[2]);
                    long lastTick = Long.parseLong(parts[3]);
                    ActiveEffect a = new ActiveEffect(e.getKey(), kind, remaining, stacks);
                    a.lastTickAt = lastTick;
                    if (parts.length >= 6) {
                        a.berserkActive = "1".equals(parts[4]);
                        a.berserkUntil = Long.parseLong(parts[5]);
                    }
                    map.put(e.getKey(), a);
                    // 특수 효과 캐시 재등록 (재시작 후 lockedSchools 등 복원)
                    EffectDef def = plugin.registry().get(e.getKey());
                    if (def != null) {
                        try {
                            // onApply는 RebornCurse 플러그인이 완전 로드된 후 호출 가능 — null check
                            if (plugin.special() != null) {
                                Player onlinePlayer = Bukkit.getPlayer(p);
                                if (onlinePlayer != null) plugin.special().onApply(onlinePlayer, def);
                            }
                        } catch (Throwable ignored) {}
                    }
                } catch (Throwable ignored) {}
            }
            if (!map.isEmpty()) active.put(p, map);
        }
    }

    private void persist(UUID p, String id, ActiveEffect a) {
        String enc = a.kind.name() + "|" + a.stacks + "|" + a.remainingTicks + "|"
                + a.lastTickAt + "|" + (a.berserkActive ? "1" : "0") + "|" + a.berserkUntil;
        RebornCore.get().kv().put(NS, p, id, enc);
    }

    public Map<String, ActiveEffect> of(UUID uuid) {
        ensureLoaded(uuid);
        return active.computeIfAbsent(uuid, k -> new HashMap<>());
    }

    public boolean apply(Player p, String id) {
        EffectDef def = plugin.registry().get(id);
        if (def == null) {
            Msg.error(p, "정의되지 않은 효과: " + id);
            return false;
        }
        Map<String, ActiveEffect> map = of(p.getUniqueId());
        ActiveEffect existing = map.get(id);
        if (existing != null) {
            if (existing.stacks >= def.maxStacks) {
                Msg.warn(p, "최대 중첩 도달: " + def.name);
                return false;
            }
            existing.stacks++;
            persist(p.getUniqueId(), id, existing);
            return true;
        }
        long ticks = def.permanent() ? -1 : def.durationSeconds;
        ActiveEffect a = new ActiveEffect(id, def.kind, ticks, 1);
        map.put(id, a);
        persist(p.getUniqueId(), id, a);

        // 영구 스탯 보정 즉시 적용
        for (var e : def.staticStats.entrySet()) {
            RebornCore.get().api().addStat(p.getUniqueId(), e.getKey(), e.getValue(),
                    "EFFECT:" + id);
        }
        for (var e : def.percentStats.entrySet()) {
            double cur = RebornCore.get().api().getStat(p.getUniqueId(), e.getKey());
            RebornCore.get().api().addStat(p.getUniqueId(), e.getKey(), cur * e.getValue(),
                    "EFFECT_PCT:" + id);
        }

        // 특수 효과 캐시 등록
        plugin.special().onApply(p, def);

        // 시그니처 룩업 — 고유 시청각 + 플레이버 텍스트
        var sig = kr.reborn.curse.signature.BlessingSignatureRegistry.lookup(id);
        if (sig != null) {
            try {
                if (sig.applyParticle != null) {
                    p.getWorld().spawnParticle(sig.applyParticle,
                            p.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.1);
                }
                if (sig.applySound != null) {
                    p.getWorld().playSound(p.getLocation(), sig.applySound, 1.0f, 1.0f);
                }
                if (sig.applyMessage != null && !sig.applyMessage.isEmpty()) {
                    Msg.send(p, sig.applyMessage);
                }
            } catch (Throwable ignored) {}
        }

        if (def.kind == EffectDef.Kind.BLESSING) {
            if (sig == null) Msg.send(p, "&b[축복] " + def.name);
            Bukkit.getPluginManager().callEvent(new RebornBlessingApplyEvent(p, def));
        } else {
            if (sig == null) Msg.send(p, "&c[저주] " + def.name);
            Bukkit.getPluginManager().callEvent(new RebornCurseApplyEvent(p, def));
        }
        return true;
    }

    public boolean cure(Player p, String id) {
        Map<String, ActiveEffect> map = of(p.getUniqueId());
        ActiveEffect a = map.remove(id);
        if (a == null) return false;
        RebornCore.get().kv().remove(NS, p.getUniqueId(), id);
        EffectDef def = plugin.registry().get(id);
        if (def != null) plugin.special().onRemove(p, def);
        // 효과 종류(축복/저주)별 다른 해제 메시지
        if (def != null) {
            String label = def.name != null ? def.name : id;
            if (def.kind == EffectDef.Kind.CURSE) {
                Msg.send(p, "&a&l✦ 저주 해제 ✦ &r&7" + label + " §a이(가) 풀렸다.");
                try {
                    p.getWorld().spawnParticle(org.bukkit.Particle.SPELL_INSTANT,
                            p.getLocation().add(0, 1.5, 0), 30, 0.5, 1, 0.5, 0.05);
                    p.playSound(p.getLocation(),
                            org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
                } catch (Throwable ignored) {}
            } else {
                Msg.send(p, "&7&l[축복 소멸] &r&7" + label + " §7의 가호가 사라졌다.");
                try {
                    p.getWorld().spawnParticle(org.bukkit.Particle.SMOKE_NORMAL,
                            p.getLocation().add(0, 1.5, 0), 20, 0.5, 0.5, 0.5);
                } catch (Throwable ignored) {}
            }
        } else {
            Msg.send(p, "&a효과 해제: " + id);
        }
        Bukkit.getPluginManager().callEvent(new RebornCurseCureEvent(p, id));
        return true;
    }

    /** 1초마다 호출 */
    public void tickAll() {
        long now = System.currentTimeMillis();
        for (var entry : active.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) continue;
            Map<String, ActiveEffect> map = entry.getValue();
            Iterator<Map.Entry<String, ActiveEffect>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                var en = it.next();
                ActiveEffect a = en.getValue();
                EffectDef def = plugin.registry().get(a.id);
                if (def == null) { it.remove(); continue; }
                // 광폭화 종료 체크
                if (a.berserkActive && now >= a.berserkUntil) {
                    a.berserkActive = false;
                }
                // tick 적용
                if (def.tickIntervalSeconds > 0
                        && now - a.lastTickAt >= def.tickIntervalSeconds * 1000L) {
                    a.lastTickAt = now;
                    applyTick(p, def, a);
                    Bukkit.getPluginManager().callEvent(new RebornCurseTickEvent(p, def));
                }
                if (!a.isPermanent()) {
                    a.remainingTicks--;
                    if (a.remainingTicks <= 0) {
                        it.remove();
                        RebornCore.get().kv().remove(NS, p.getUniqueId(), a.id);
                        plugin.special().onRemove(p, def);
                        Msg.send(p, "&7" + def.name + " 효과가 만료되었다.");
                    } else if (a.remainingTicks % 60 == 0) {
                        // 1분마다 잔여시간 저장 (재시작 시 1분 단위 정확도)
                        persist(p.getUniqueId(), a.id, a);
                    }
                }
            }
        }
    }

    private void applyTick(Player p, EffectDef def, ActiveEffect a) {
        UUID id = p.getUniqueId();
        for (var e : def.tickStats.entrySet()) {
            RebornCore.get().api().addStat(id, e.getKey(), e.getValue() * a.stacks, "TICK:" + def.id);
        }
        if (def.tickStatsCommon != 0) {
            for (StatType t : StatType.COMMON_8) {
                RebornCore.get().api().addStat(id, t, def.tickStatsCommon * a.stacks,
                        "TICK_C:" + def.id);
            }
        }
        // SpecialEffectEngine 위임 — hp_tick, out_of_ship, stats_tick_day_only, npc_favor_tick
        plugin.special().applyTick(p, def, a);

        // 시그니처 tick 입자 — 활성 중 주기적 시각 효과
        var sig = kr.reborn.curse.signature.BlessingSignatureRegistry.lookup(def.id);
        if (sig != null && sig.tickParticle != null && sig.tickParticleCount > 0) {
            try {
                p.getWorld().spawnParticle(sig.tickParticle,
                        p.getLocation().add(0, 1, 0), sig.tickParticleCount,
                        0.3, 0.8, 0.3, 0.02);
            } catch (Throwable ignored) {}
        }

        // 광폭화 발동 — BerserkEngine 위임
        if (def.berserkChance > 0 && Rand.chance(def.berserkChance) && !a.berserkActive) {
            plugin.berserk().start(p, a);
            Bukkit.getPluginManager().callEvent(new RebornBerserkEvent(p, def));
        }
    }
}
