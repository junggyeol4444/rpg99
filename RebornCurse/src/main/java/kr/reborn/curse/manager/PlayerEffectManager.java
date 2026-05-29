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

    private final RebornCurse plugin;
    private final Map<UUID, Map<String, ActiveEffect>> active = new ConcurrentHashMap<>();

    public PlayerEffectManager(RebornCurse plugin) {
        this.plugin = plugin;
    }

    public Map<String, ActiveEffect> of(UUID uuid) {
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
            return true;
        }
        long ticks = def.permanent() ? -1 : def.durationSeconds;
        ActiveEffect a = new ActiveEffect(id, def.kind, ticks, 1);
        map.put(id, a);

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

        if (def.kind == EffectDef.Kind.BLESSING) {
            Msg.send(p, "&b[축복] " + def.name);
            Bukkit.getPluginManager().callEvent(new RebornBlessingApplyEvent(p, def));
        } else {
            Msg.send(p, "&c[저주] " + def.name);
            Bukkit.getPluginManager().callEvent(new RebornCurseApplyEvent(p, def));
        }
        return true;
    }

    public boolean cure(Player p, String id) {
        Map<String, ActiveEffect> map = of(p.getUniqueId());
        ActiveEffect a = map.remove(id);
        if (a == null) return false;
        EffectDef def = plugin.registry().get(id);
        if (def != null) plugin.special().onRemove(p, def);
        Msg.send(p, "&a효과 해제: " + id);
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
                        plugin.special().onRemove(p, def);
                        Msg.send(p, "&7" + def.name + " 효과가 만료되었다.");
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

        // 광폭화 발동 — BerserkEngine 위임
        if (def.berserkChance > 0 && Rand.chance(def.berserkChance) && !a.berserkActive) {
            plugin.berserk().start(p, a);
            Bukkit.getPluginManager().callEvent(new RebornBerserkEvent(p, def));
        }
    }
}
