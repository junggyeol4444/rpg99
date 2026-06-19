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

    private static final String NS = "RebornQuest.active";

    private final RebornQuest plugin;
    private final ConcurrentHashMap<UUID, Map<String, Progress>> active = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> loaded = ConcurrentHashMap.newKeySet();

    public QuestEngine(RebornQuest p) { this.plugin = p; }

    private void ensureLoaded(UUID p) {
        if (loaded.add(p)) {
            var all = RebornCore.get().kv().loadAll(NS, p);
            Map<String, Progress> map = new HashMap<>();
            for (var e : all.entrySet()) {
                try {
                    String[] parts = e.getValue().split(":", 2);
                    Progress pr = new Progress();
                    pr.count = Integer.parseInt(parts[0]);
                    pr.phase = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                    map.put(e.getKey(), pr);
                } catch (Throwable ignored) {}
            }
            if (!map.isEmpty()) active.put(p, map);
        }
    }

    private void persist(UUID p, String questId, Progress pr) {
        RebornCore.get().kv().put(NS, p, questId, pr.count + ":" + pr.phase);
    }

    public boolean accept(Player p, String questId) {
        Quest q = plugin.registry().get(questId);
        if (q == null) return false;
        ensureLoaded(p.getUniqueId());
        Map<String, Progress> map = active.computeIfAbsent(p.getUniqueId(), x -> new HashMap<>());
        if (map.containsKey(questId)) { Msg.warn(p, "이미 진행 중인 퀘스트."); return false; }
        Progress pr = new Progress();
        map.put(questId, pr);
        persist(p.getUniqueId(), questId, pr);
        Objective obj = currentObjective(q, map.get(questId));
        Msg.send(p, "&a퀘스트 수락: &f" + q.name);
        announce(p, q, obj, map.get(questId));
        return true;
    }

    public boolean abandon(Player p, String questId) {
        ensureLoaded(p.getUniqueId());
        Map<String, Progress> map = active.get(p.getUniqueId());
        if (map == null || map.remove(questId) == null) return false;
        RebornCore.get().kv().remove(NS, p.getUniqueId(), questId);
        Msg.warn(p, "퀘스트 포기: " + questId);
        return true;
    }

    public boolean has(UUID id, String questId) {
        ensureLoaded(id);
        Map<String, Progress> map = active.get(id);
        return map != null && map.containsKey(questId);
    }

    // ───────────────────────── 진행 ─────────────────────────

    /**
     * 사건 1건을 모든 활성 퀘스트에 반영. 현재 목표와 타입·대상이 맞으면 카운트 증가.
     * 스냅샷을 순회하므로 complete()가 맵을 수정해도 안전.
     */
    public void progress(Player p, String type, String target, int n) {
        ensureLoaded(p.getUniqueId());
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
            // WORLD 퀘스트는 기여도 트래커에 누적 (보상 분배 시 사용)
            if ("WORLD".equalsIgnoreCase(q.type) || (q.world != null && !q.world.isEmpty())) {
                plugin.contrib().add(q.id, p.getUniqueId(), n);
            }
            if (prog.count >= obj.amount) advance(p, q, prog);
            else { persist(p.getUniqueId(), e.getKey(), prog); announce(p, q, obj, prog); }
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
        ensureLoaded(p.getUniqueId());
        Map<String, Progress> map = active.get(p.getUniqueId());
        if (map == null) return;
        for (Map.Entry<String, Progress> e : map.entrySet()) {
            Quest q = plugin.registry().get(e.getKey());
            if (q == null) continue;
            if ("SURVIVE".equalsIgnoreCase(currentObjective(q, e.getValue()).type)) {
                e.getValue().count = 0;
                persist(p.getUniqueId(), e.getKey(), e.getValue());
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
            persist(p.getUniqueId(), q.id, prog);
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
        RebornCore.get().kv().remove(NS, p.getUniqueId(), q.id);
        // 영구 완료 마커 — PlayerData.status에 "quest_complete:<id>" 기록
        // 외부 시스템(예: /element이 ancient_spirit_test 통과 확인)이 참조
        try {
            var d = RebornCore.get().api().getPlayerData(p.getUniqueId());
            if (d != null) {
                d.status().put("quest_complete:" + q.id,
                        new kr.reborn.core.data.PlayerData.StatusEffect(
                                "quest_complete:" + q.id, "QUEST", Long.MAX_VALUE, 1));
            }
        } catch (Throwable ignored) {}
        // 퀘스트 종류·세계별 고유 완료 연출
        renderCompletion(p, q);
        applyRewards(p, q);
        Bukkit.getPluginManager().callEvent(new RebornQuestCompleteEvent(p, q.id));
        if (q.linkedQuestId != null && !q.linkedQuestId.isEmpty()) {
            Quest linked = plugin.registry().get(q.linkedQuestId);
            if (linked != null && !has(p.getUniqueId(), q.linkedQuestId)) {
                accept(p, q.linkedQuestId);  // 후속 퀘스트 자동 수락
            }
        }
    }

    /** 퀘스트 종류·세계별 다른 완료 연출. */
    private void renderCompletion(Player p, Quest q) {
        String type = q.type == null ? "" : q.type.toUpperCase();
        String world = q.world == null ? "" : q.world;
        org.bukkit.Sound sound;
        org.bukkit.Particle particle;
        String msg;
        switch (type) {
            case "KILL" -> {
                sound = org.bukkit.Sound.ENTITY_PLAYER_LEVELUP;
                particle = org.bukkit.Particle.CRIT;
                msg = "&c&l⚔ 퀘스트 완료: &r&f" + q.name;
            }
            case "GATHER" -> {
                sound = org.bukkit.Sound.ENTITY_VILLAGER_YES;
                particle = org.bukkit.Particle.VILLAGER_HAPPY;
                msg = "&a&l📦 수집 완료: &r&f" + q.name;
            }
            case "TALK" -> {
                sound = org.bukkit.Sound.ENTITY_PLAYER_LEVELUP;
                particle = org.bukkit.Particle.HEART;
                msg = "&d&l💬 대화 완료: &r&f" + q.name;
            }
            case "ESCORT" -> {
                sound = org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE;
                particle = org.bukkit.Particle.END_ROD;
                msg = "&e&l🛡 호위 완료: &r&f" + q.name;
            }
            case "CRAFT" -> {
                sound = org.bukkit.Sound.BLOCK_ANVIL_USE;
                particle = org.bukkit.Particle.SPELL_INSTANT;
                msg = "&6&l⚒ 제작 완료: &r&f" + q.name;
            }
            case "DELIVER" -> {
                sound = org.bukkit.Sound.ENTITY_VILLAGER_TRADE;
                particle = org.bukkit.Particle.HEART;
                msg = "&e&l📜 배달 완료: &r&f" + q.name;
            }
            case "EXPLORE" -> {
                sound = org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE;
                particle = org.bukkit.Particle.PORTAL;
                msg = "&b&l🗺 탐험 완료: &r&f" + q.name;
            }
            case "SURVIVE" -> {
                sound = org.bukkit.Sound.ITEM_TOTEM_USE;
                particle = org.bukkit.Particle.TOTEM;
                msg = "&7&l⌛ 생존 완료: &r&f" + q.name;
            }
            case "DEFEND" -> {
                sound = org.bukkit.Sound.BLOCK_BEACON_ACTIVATE;
                particle = org.bukkit.Particle.END_ROD;
                msg = "&9&l🛡 방어 완료: &r&f" + q.name;
            }
            case "WORLD" -> {
                sound = org.bukkit.Sound.BLOCK_BELL_RESONATE;
                particle = org.bukkit.Particle.TOTEM;
                msg = "&6&l✦ 세계 퀘스트 완료: &r&f" + q.name;
                // 월드 퀘스트는 전체 broadcast
                org.bukkit.Bukkit.broadcastMessage("§6§l[" + world + " 세계 퀘스트] §f"
                        + p.getName() + " §7가 " + q.name + " 완료!");
            }
            case "SKILL_USE" -> {
                sound = org.bukkit.Sound.BLOCK_ENCHANTMENT_TABLE_USE;
                particle = org.bukkit.Particle.CRIT_MAGIC;
                msg = "&5&l✺ 스킬 수련: &r&f" + q.name;
            }
            default -> {
                sound = org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE;
                particle = org.bukkit.Particle.TOTEM;
                msg = "&6&l퀘스트 완료: &r&f" + q.name;
            }
        }
        Msg.send(p, msg);
        try {
            p.getWorld().playSound(p.getLocation(), sound, 1.0f, 1.0f);
            p.getWorld().spawnParticle(particle, p.getLocation().add(0, 1.5, 0), 50, 0.5, 0.8, 0.5, 0.1);
            p.sendTitle("§6✦ 완료 ✦", "§f" + q.name, 10, 40, 20);
        } catch (Throwable ignored) {}
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
        // 화폐 보상 (gold 또는 money: { currency, amount } 형식 둘 다 지원)
        Object gold = q.rewards.get("gold");
        Object money = q.rewards.get("money");
        String currency = "GOLD_COIN";
        long amount = 0;
        if (gold instanceof Number n) amount = n.longValue();
        else if (money instanceof Map<?, ?> mm) {
            Object cur = mm.get("currency");
            Object amt = mm.get("amount");
            if (cur != null) currency = String.valueOf(cur);
            if (amt instanceof Number nn) amount = nn.longValue();
        }
        if (amount > 0) {
            try {
                var ep = org.bukkit.Bukkit.getPluginManager().getPlugin("RebornEconomy");
                if (ep != null) {
                    Object cm = ep.getClass().getMethod("currencies").invoke(ep);
                    cm.getClass().getMethod("deposit", java.util.UUID.class, String.class, long.class)
                            .invoke(cm, p.getUniqueId(), currency, amount);
                }
                Msg.send(p, "&6보상: §f+" + amount + " " + currency);
            } catch (Throwable ignored) {}
        }
        // 단일 아이템 (item: <customId>)
        Object singleItem = q.rewards.get("item");
        if (singleItem != null) {
            String id = String.valueOf(singleItem);
            // RebornCraft ItemRegistry에서 실제 ItemStack 생성 시도
            boolean given = false;
            try {
                var cp = org.bukkit.Bukkit.getPluginManager().getPlugin("RebornCraft");
                if (cp != null) {
                    Object items = cp.getClass().getMethod("items").invoke(cp);
                    Object def = items.getClass().getMethod("get", String.class)
                            .invoke(items, id);
                    if (def != null) {
                        Object stack = items.getClass().getMethod("render", def.getClass())
                                .invoke(items, def);
                        if (stack instanceof org.bukkit.inventory.ItemStack is) {
                            p.getInventory().addItem(is);
                            given = true;
                        }
                    }
                }
            } catch (Throwable ignored) {}
            // fallback: Material로 시도
            if (!given) {
                org.bukkit.Material mat = org.bukkit.Material.matchMaterial(id.toUpperCase());
                if (mat != null) {
                    p.getInventory().addItem(new org.bukkit.inventory.ItemStack(mat, 1));
                    given = true;
                }
            }
            Msg.send(p, given ? "&6보상: §f" + id + " §7지급됨"
                              : "&6보상: §f" + id + " §c(아이템 시스템 미연결)");
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

    public Map<String, Progress> activeFor(UUID id) { ensureLoaded(id); return active.getOrDefault(id, Map.of()); }

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
