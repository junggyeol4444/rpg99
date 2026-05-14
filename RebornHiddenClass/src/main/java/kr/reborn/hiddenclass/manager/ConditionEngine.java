package kr.reborn.hiddenclass.manager;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import kr.reborn.hiddenclass.RebornHiddenClass;
import kr.reborn.hiddenclass.data.Condition;
import kr.reborn.hiddenclass.data.HiddenClass;
import kr.reborn.hiddenclass.event.RebornHiddenClassUnlockEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class ConditionEngine {

    private final RebornHiddenClass plugin;

    public ConditionEngine(RebornHiddenClass plugin) {
        this.plugin = plugin;
    }

    /** 초기 환생 시 1회 호출 — INITIAL 클래스 후보 굴림. */
    public void rollInitial(Player p, WorldKey world) {
        for (HiddenClass hc : plugin.registry().all()) {
            if (hc.type != HiddenClass.Type.INITIAL) continue;
            if (hc.worldRestriction != null && hc.worldRestriction != world) continue;
            if (plugin.progress().has(p.getUniqueId(), hc.id)) continue;
            if (Rand.chance(hc.initialChance)) {
                unlock(p, hc);
            }
        }
    }

    /** 이벤트나 로그인 시 풀 체크 — ACHIEVEMENT만. */
    public void fullCheck(Player p) {
        for (HiddenClass hc : plugin.registry().all()) {
            if (hc.type != HiddenClass.Type.ACHIEVEMENT) continue;
            if (plugin.progress().has(p.getUniqueId(), hc.id)) continue;
            if (matches(p, hc)) unlock(p, hc);
        }
    }

    public boolean matches(Player p, HiddenClass hc) {
        UUID id = p.getUniqueId();
        var data = RebornCore.get().api().getPlayerData(id);
        if (data == null) return false;
        for (Condition c : hc.conditions) {
            if (!check(p, c, data)) return false;
        }
        return true;
    }

    private boolean check(Player p, Condition c, kr.reborn.core.data.PlayerData data) {
        UUID id = p.getUniqueId();
        switch (c.type) {
            case STAT_MIN:
                if (c.stat == null) return false;
                return data.getStat(c.stat) >= c.numericValue;
            case TIER_REACHED:
                return data.tier() != null && data.tier().equals(c.stringValue);
            case WORLDS_VISITED:
                return data.visited().size() >= c.numericValue;
            case KILL_COUNT:
                return plugin.progress().kills(id) >= c.numericValue;
            case TRADE_COUNT:
                return plugin.progress().trades(id) >= c.numericValue;
            case QUEST_COMPLETE:
                return plugin.progress().quests(id).contains(c.stringValue);
            case NPC_FAVOR:
                return plugin.progress().favor(id, c.stringValue) >= c.numericValue;
            case CLAN_RANK:
                // TODO: RebornClan API hook
                return false;
            case ADMIN_GRANT:
                return false;
            default:
                return false;
        }
    }

    public void unlock(Player p, HiddenClass hc) {
        plugin.progress().markUnlocked(p.getUniqueId(), hc.id);
        // 효과 적용
        for (var e : hc.statBonuses.entrySet()) {
            RebornCore.get().api().addStat(p.getUniqueId(), e.getKey(), e.getValue(),
                    "HC:" + hc.id);
        }
        for (var e : hc.statOverrides.entrySet()) {
            RebornCore.get().api().setStat(p.getUniqueId(), e.getKey(), e.getValue());
        }
        // 해금 연출
        try {
            p.sendTitle(Msg.c("&6&l히든 클래스 해금!"), Msg.c(hc.name), 10, 60, 20);
        } catch (Throwable ignored) {}
        Bukkit.broadcastMessage(Msg.PREFIX + Msg.c(
                "&6&l[히든] " + p.getName() + " 님이 &r" + hc.name + " &6&l을(를) 해금했다!"));
        Bukkit.getPluginManager().callEvent(new RebornHiddenClassUnlockEvent(p, hc));
    }

    public boolean adminGrant(Player p, String id) {
        HiddenClass hc = plugin.registry().get(id);
        if (hc == null) return false;
        if (plugin.progress().has(p.getUniqueId(), id)) return false;
        unlock(p, hc);
        return true;
    }
}
