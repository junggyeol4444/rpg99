package kr.reborn.quest.engine;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.StatType;
import kr.reborn.core.event.RebornQuestCompleteEvent;
import kr.reborn.core.util.Msg;
import kr.reborn.quest.RebornQuest;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 퀘스트 진행 엔진.
 *
 * 모든 퀘스트 타입(KILL/GATHER/CRAFT/TALK/EXPLORE/SURVIVE/CUSTOM…)과 다단계 WORLD
 * 퀘스트를 하나의 진행 경로 progress()로 처리한다. 리스너는 사건만 알려주면 된다.
 *
 * 다단계 퀘스트: phases가 있으면 현재 단계(phase)의 목표를 따르고, 단계 완료 시
 * 다음 단계로, 마지막 단계 완료 시 퀘스트 완료.
 */
public final class QuestEngine {

    private final RebornQuest plugin;
    private final ConcurrentHashMap<UUID, Map<String, Progress>> active = new ConcurrentHashMap<>();

    public QuestEngine(RebornQuest p) { this.plugin = p; }

    public boolean accept(Player p, String questId) {
        Quest q = plugin.registry().get(questId);
        if (q == null) return false;
        Map<String, Progress> map = active.computeIfAbsent(p.getUniqueId(), x -> new HashMap<>());
        if (map.containsKey(questId)) { Msg.warn(p, "이미 진행 중인 퀘스트."); return false; }
        map.put(questId, new Progress());
        Objective obj = currentObjective(q, map.get(questId));
        Msg.send(p, "&a퀘스트 수락: &f" + q.name);
        announce(p, q, obj, map.get(questId));
        return true;
    }

    public boolean abandon(Player p, String questId) {
        Map<String, Progress> map = active.get(p.getUniqueId());
        if (map == null || map.remove(questId) == null) return false;
        Msg.warn(p, "퀘스트 포기: " + questId);
        return true;
    }

    public boolean has(UUID id, String questId) {
        Map<String, Progress> map = active.get(id);
        return map != null && map.containsKey(questId);
    }

    // ───────────────────────── 진행 ─────────────────────────

    /**
     * 사건 1건을 모든 활성 퀘스트에 반영. 현재 목표와 타입·대상이 맞으면 카운트 증가.
     * 스냅샷을 순회하므로 complete()가 맵을 수정해도 안전.
     */
    public void progress(Player p, String type, String target, int n) {
        Map<String, Progress> map = active.get(p.getUniqueId());
        if (map == null || map.isEmpty()) return;
        for (Map.Entry<String, Progress> e : new ArrayList<>(map.entrySet())) {
            Quest q = plugin.registry().get(e.getKey());
            if (q == null) continue;
            Progress prog = e.getValue();
            Objective obj = currentObjective(q, prog);
            if (!obj.type.equalsIgnoreCase(type)) continue;
            if (!matchesTarget(obj.target, target)) continue;
            prog.count += n;
            if (prog.count >= obj.amount) advance(p, q, prog);
            else announce(p, q, obj, prog);
        }
    }

    /** 다른 플러그인/명령에서 임의 진행을 밀어넣는 진입점 (ESCORT/DEFEND/SKILL_USE 등). */
    public void custom(Player p, String key, int n) { progress(p, "CUSTOM", key, n); }

    /** 1초마다 호출 — 생존(SURVIVE) 퀘스트 누적. */
    public void tickSurvive() {
        for (UUID id : active.keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && !p.isDead()) progress(p, "SURVIVE", p.getWorld().getName(), 1);
        }
    }

    /** 사망 시 — 생존 퀘스트 진행 초기화. */
    public void onPlayerDeath(Player p) {
        Map<String, Progress> map = active.get(p.getUniqueId());
        if (map == null) return;
        for (Map.Entry<String, Progress> e : map.entrySet()) {
            Quest q = plugin.registry().get(e.getKey());
            if (q == null) continue;
            if ("SURVIVE".equalsIgnoreCase(currentObjective(q, e.getValue()).type)) {
                e.getValue().count = 0;
                Msg.warn(p, "&c생존 퀘스트 진행이 초기화되었다: " + q.name);
            }
        }
    }

    private Objective currentObjective(Quest q, Progress prog) {
        if (!q.phases.isEmpty()) {
            int idx = Math.min(prog.phase, q.phases.size() - 1);
            Quest.Phase ph = q.phases.get(idx);
            return new Objective(ph.target, ph.targetId, ph.amount, ph.name);
        }
        return new Objective(q.type, q.target, q.amount, q.type);
    }

    private boolean matchesTarget(String objTarget, String actual) {
        if (objTarget == null || objTarget.isEmpty()) return true;  // 대상 무관
        return objTarget.equalsIgnoreCase(actual);
    }

    private void advance(Player p, Quest q, Progress prog) {
        if (!q.phases.isEmpty() && prog.phase < q.phases.size() - 1) {
            prog.phase++;
            prog.count = 0;
            Quest.Phase next = q.phases.get(prog.phase);
            Msg.send(p, "&e[" + stripColor(q.name) + "] 단계 완료! 다음: &f" + next.name);
            p.getWorld().playSound(p.getLocation(),
                    org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
        } else {
            complete(p, q);
        }
    }

    public void complete(Player p, Quest q) {
        Map<String, Progress> map = active.get(p.getUniqueId());
        if (map != null) map.remove(q.id);
        Msg.send(p, "&6&l퀘스트 완료: &r&f" + q.name);
        p.getWorld().playSound(p.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        applyRewards(p, q);
        Bukkit.getPluginManager().callEvent(new RebornQuestCompleteEvent(p, q.id));
        if (q.linkedQuestId != null && !q.linkedQuestId.isEmpty()) {
            Quest linked = plugin.registry().get(q.linkedQuestId);
            if (linked != null && !has(p.getUniqueId(), q.linkedQuestId)) {
                accept(p, q.linkedQuestId);  // 후속 퀘스트 자동 수락
            }
        }
    }

    private void applyRewards(Player p, Quest q) {
        Object stats = q.rewards.get("stat");
        if (stats instanceof Map<?, ?> m) {
            for (var e : m.entrySet()) {
                try {
                    StatType t = StatType.valueOf(String.valueOf(e.getKey()));
                    double v = ((Number) e.getValue()).doubleValue();
                    RebornCore.get().api().addStat(p.getUniqueId(), t, v, "quest:" + q.id);
                    Msg.send(p, "&7보상: §a" + t + " +" + (int) v);
                } catch (Exception ignored) {}
            }
        }
        // 칭호 부여 — RebornTitle 리플렉션
        Object title = q.rewards.get("title");
        if (title != null) {
            try {
                var tp = org.bukkit.Bukkit.getPluginManager().getPlugin("RebornTitle");
                if (tp != null) {
                    Object tm = tp.getClass().getMethod("titles").invoke(tp);
                    tm.getClass().getMethod("grant", Player.class, String.class)
                            .invoke(tm, p, String.valueOf(title));
                }
            } catch (Throwable ignored) {}
            Msg.send(p, "&6칭호 획득: " + title);
        }
        // 화폐 보상
        Object gold = q.rewards.get("gold");
        if (gold instanceof Number n) {
            try {
                var ep = org.bukkit.Bukkit.getPluginManager().getPlugin("RebornEconomy");
                if (ep != null) {
                    Object cur = ep.getClass().getMethod("currencies").invoke(ep);
                    cur.getClass().getMethod("deposit", java.util.UUID.class, String.class, long.class)
                            .invoke(cur, p.getUniqueId(), "GOLD_COIN", n.longValue());
                }
                Msg.send(p, "&6보상: §f+" + n.longValue() + " GOLD");
            } catch (Throwable ignored) {}
        }
        // 아이템 보상
        Object items = q.rewards.get("items");
        if (items instanceof java.util.List<?> list) {
            for (Object raw : list) {
                if (!(raw instanceof Map<?, ?> mm)) continue;
                try {
                    org.bukkit.Material mat = org.bukkit.Material.matchMaterial(
                            String.valueOf(mm.get("material")));
                    int amt = ((Number) mm.getOrDefault("amount", 1)).intValue();
                    if (mat != null) {
                        p.getInventory().addItem(new org.bukkit.inventory.ItemStack(mat, amt));
                        Msg.send(p, "&6보상: §f" + mat + " ×" + amt);
                    }
                } catch (Throwable ignored) {}
            }
        }
        // 스킬 학습 — RebornSkill
        Object skills = q.rewards.get("skills");
        if (skills instanceof java.util.List<?> list) {
            try {
                var sp = org.bukkit.Bukkit.getPluginManager().getPlugin("RebornSkill");
                if (sp != null) {
                    for (Object skillId : list) {
                        sp.getClass().getMethod("learnByApi",
                                        java.util.UUID.class, String.class)
                                .invoke(sp, p.getUniqueId(), String.valueOf(skillId));
                        Msg.send(p, "&b스킬 학습: §f" + skillId);
                    }
                }
            } catch (Throwable ignored) {}
        }
        // 축복/저주 부여
        Object blessing = q.rewards.get("blessing");
        if (blessing != null) applyEffect(p, String.valueOf(blessing));
        Object curse = q.rewards.get("curse");
        if (curse != null) applyEffect(p, String.valueOf(curse));
    }

    private void applyEffect(Player p, String effectId) {
        try {
            var cp = org.bukkit.Bukkit.getPluginManager().getPlugin("RebornCurse");
            if (cp != null) {
                Object effects = cp.getClass().getMethod("effects").invoke(cp);
                effects.getClass().getMethod("apply", Player.class, String.class)
                        .invoke(effects, p, effectId);
            }
        } catch (Throwable ignored) {}
    }

    // ───────────────────────── 표시 ─────────────────────────

    private void announce(Player p, Quest q, Objective obj, Progress prog) {
        String phaseTag = q.phases.isEmpty() ? "" : " §8(" + (prog.phase + 1) + "/" + q.phases.size() + ")";
        actionBar(p, "§e" + stripColor(q.name) + phaseTag
                + " §7" + obj.label + " §f" + prog.count + "/" + obj.amount);
    }

    private void actionBar(Player p, String text) {
        try {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(text));
        } catch (Throwable ignored) {}
    }

    private String stripColor(String s) { return s == null ? "" : s.replaceAll("&.|§.", ""); }

    public Map<String, Progress> activeFor(UUID id) { return active.getOrDefault(id, Map.of()); }

    /** 진행 중 퀘스트 1개의 단계 인지(認知) 진행 문자열. 없으면 null. */
    public String describe(UUID id, String questId) {
        Map<String, Progress> map = active.get(id);
        if (map == null) return null;
        Progress prog = map.get(questId);
        if (prog == null) return null;
        Quest q = plugin.registry().get(questId);
        if (q == null) return null;
        Objective obj = currentObjective(q, prog);
        String phaseTag = q.phases.isEmpty() ? ""
                : " §8[" + (prog.phase + 1) + "/" + q.phases.size() + " " + obj.label + "]";
        return "§f" + q.name + phaseTag + " §7" + prog.count + "/" + obj.amount;
    }

    /** 진행 상태 — 현재 단계 index + 단계 내 카운트. */
    public static final class Progress {
        public int count;
        public int phase;
        public Progress() {}
    }

    /** 현재 달성해야 할 목표 (타입·대상·수량·표시명). */
    private static final class Objective {
        final String type, target, label;
        final int amount;
        Objective(String type, String target, int amount, String label) {
            this.type = type == null ? "" : type;
            this.target = target;
            this.amount = Math.max(1, amount);
            this.label = label == null ? "" : label;
        }
    }
}
