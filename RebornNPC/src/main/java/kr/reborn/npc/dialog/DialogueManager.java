package kr.reborn.npc.dialog;

import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Msg;
import kr.reborn.npc.RebornNPC;
import kr.reborn.npc.emotion.Emotion;
import kr.reborn.npc.entity.RebornNpc;
import kr.reborn.npc.soul.Memory;
import kr.reborn.npc.soul.Personality;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 대화 세션 매니저.
 *
 * 한 플레이어는 동시에 1개의 활성 세션을 가짐.
 * /npc say <choice> 명령 또는 1/2/3 채팅으로 선택지 선택.
 * 노드 도달 시:
 *   1. 조건 평가 → 실패 시 fallback (없으면 root)
 *   2. 액션 실행 (addFavor, giveItem, giveQuest, addMemory 등)
 *   3. 대사 + 선택지 표시
 *
 * 조건 / 액션 grammar:
 *   conditions:
 *     "favor>=N" — NPC 관계 호감도 N 이상
 *     "personality:TRAIT>=N"
 *     "hasItem:material:N" — 인벤 보유
 *     "stat:STAT_TYPE>=N"
 *     "memory:KIND" — 기억 보유 (예: HELPED_ME)
 *   actions:
 *     "addFavor:N" — 호감 +N
 *     "addEmotion:KIND:N"
 *     "giveItem:material:N"
 *     "giveQuest:questId"
 *     "addMemory:KIND:N:label"
 *     "stat:STAT_TYPE:+N"
 *     "broadcast:text"
 *     "endDialog"
 */
public final class DialogueManager {

    private final RebornNPC plugin;
    /** uuid → 활성 세션 */
    private final Map<UUID, DialogueSession> active = new ConcurrentHashMap<>();

    public DialogueManager(RebornNPC plugin) { this.plugin = plugin; }

    public void open(Player p, RebornNpc npc) {
        // dialogId가 있으면 우선, 없으면 job 매핑
        Dialogue d = npc.dialogId != null && !npc.dialogId.isEmpty()
                ? plugin.dialogues().get(npc.dialogId)
                : plugin.dialogues().resolveForJob(npc.job);
        if (d == null) {
            // 디폴트 인사만 (NpcInteractListener의 기존 인사로 fallback)
            return;
        }
        DialogueNode start = d.root();
        if (start == null) return;
        DialogueSession ses = new DialogueSession(p.getUniqueId(), npc.id, d, start);
        active.put(p.getUniqueId(), ses);
        show(p, npc, ses);
    }

    public void choose(Player p, int idx) {
        DialogueSession ses = active.get(p.getUniqueId());
        if (ses == null || ses.ended()) {
            Msg.warn(p, "활성 대화 없음.");
            return;
        }
        if (ses.current == null) { end(p); return; }
        // 표시된 선택지에서 idx 번째 (조건 통과한 것만)
        var visible = visibleChoices(p, ses);
        if (idx < 0 || idx >= visible.size()) {
            Msg.warn(p, "잘못된 선택지 번호.");
            return;
        }
        DialogueChoice c = visible.get(idx);
        // 액션 실행
        RebornNpc npc = plugin.registry().get(ses.npcId);
        for (String a : c.actions) executeAction(p, npc, a);
        // 다음 노드
        if (c.nextNodeId == null) { end(p); return; }
        DialogueNode next = ses.dialogue.node(c.nextNodeId);
        if (next == null) { end(p); return; }
        // 노드 조건
        if (!evaluateConditions(p, npc, next.conditions)) {
            DialogueNode fb = next.fallbackNodeId != null ? ses.dialogue.node(next.fallbackNodeId) : null;
            if (fb == null) { end(p); return; }
            next = fb;
        }
        ses.current = next;
        // 노드 도달 액션
        for (String a : next.actions) executeAction(p, npc, a);
        show(p, npc, ses);
    }

    public void end(Player p) {
        DialogueSession ses = active.remove(p.getUniqueId());
        if (ses == null) return;
        Msg.send(p, "&7대화 종료.");
    }

    public boolean inSession(UUID p) {
        DialogueSession ses = active.get(p);
        return ses != null && !ses.ended();
    }

    private void show(Player p, RebornNpc npc, DialogueSession ses) {
        if (ses.current == null) { end(p); return; }
        String name = npc != null ? npc.displayName : "?";
        // autoNext 처리 — 선택지 없고 autoNext 있으면 1초 뒤 자동 진행
        var visible = visibleChoices(p, ses);
        Msg.send(p, "&6[" + name + "] &f" + ses.current.text);
        if (visible.isEmpty()) {
            if (ses.current.autoNext != null) {
                final String nextId = ses.current.autoNext;
                RebornCore.get().scheduler().runTaskLater(() -> {
                    DialogueSession s2 = active.get(p.getUniqueId());
                    if (s2 != ses) return;
                    DialogueNode n2 = ses.dialogue.node(nextId);
                    if (n2 == null) { end(p); return; }
                    ses.current = n2;
                    for (String a : n2.actions) executeAction(p, npc, a);
                    show(p, npc, ses);
                }, 30L);
            } else {
                end(p);
            }
            return;
        }
        for (int i = 0; i < visible.size(); i++) {
            DialogueChoice c = visible.get(i);
            Msg.send(p, "&e" + (i + 1) + ". &f" + c.label);
        }
        Msg.send(p, "&7숫자 입력 또는 /npc say <번호> 로 선택.");
    }

    private java.util.List<DialogueChoice> visibleChoices(Player p, DialogueSession ses) {
        RebornNpc npc = plugin.registry().get(ses.npcId);
        var out = new java.util.ArrayList<DialogueChoice>();
        for (DialogueChoice c : ses.current.choices) {
            if (evaluateConditions(p, npc, c.conditions)) out.add(c);
        }
        return out;
    }

    /* ───────────────── 조건 평가 ───────────────── */
    private boolean evaluateConditions(Player p, RebornNpc npc, java.util.List<String> conditions) {
        if (conditions == null || conditions.isEmpty()) return true;
        for (String c : conditions) {
            if (!evalOne(p, npc, c)) return false;
        }
        return true;
    }

    private boolean evalOne(Player p, RebornNpc npc, String cond) {
        try {
            if (cond.startsWith("favor")) {
                String[] parts = splitOp(cond.substring(5));
                double val = Double.parseDouble(parts[1]);
                double favor = npc.relations.player(p.getUniqueId());
                return compare(favor, parts[0], val);
            }
            if (cond.startsWith("personality:")) {
                // personality:TRAIT>=N
                String body = cond.substring("personality:".length());
                int idx = findOp(body);
                String traitName = body.substring(0, idx).trim();
                String op = opAt(body, idx);
                double val = Double.parseDouble(body.substring(idx + op.length()).trim());
                if (npc.soul == null) return false;
                Personality.Trait t = Personality.Trait.valueOf(traitName.toUpperCase());
                return compare(npc.soul.personality.get(t), op, val);
            }
            if (cond.startsWith("memory:")) {
                String kind = cond.substring("memory:".length()).trim();
                if (npc.soul == null) return false;
                Memory.Kind k = Memory.Kind.valueOf(kind.toUpperCase());
                return npc.soul.memory.has(p.getUniqueId().toString(), k);
            }
            if (cond.startsWith("hasItem:")) {
                String body = cond.substring("hasItem:".length());
                String[] sp = body.split(":");
                org.bukkit.Material mat = org.bukkit.Material.matchMaterial(sp[0]);
                if (mat == null) return false;
                int needed = sp.length >= 2 ? Integer.parseInt(sp[1]) : 1;
                int have = 0;
                for (org.bukkit.inventory.ItemStack it : p.getInventory().getContents()) {
                    if (it != null && it.getType() == mat) have += it.getAmount();
                }
                return have >= needed;
            }
            if (cond.startsWith("stat:")) {
                String body = cond.substring("stat:".length());
                int idx = findOp(body);
                String statName = body.substring(0, idx).trim();
                String op = opAt(body, idx);
                double val = Double.parseDouble(body.substring(idx + op.length()).trim());
                kr.reborn.core.data.StatType st = kr.reborn.core.data.StatType.valueOf(statName.toUpperCase());
                double cur = RebornCore.get().api().getStat(p.getUniqueId(), st);
                return compare(cur, op, val);
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private String[] splitOp(String s) {
        int idx = findOp(s);
        String op = opAt(s, idx);
        return new String[]{op, s.substring(idx + op.length()).trim()};
    }

    private int findOp(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '>' || c == '<' || c == '=' || c == '!') return i;
        }
        return s.length();
    }

    private String opAt(String s, int idx) {
        if (idx >= s.length()) return "==";
        char c = s.charAt(idx);
        char n = idx + 1 < s.length() ? s.charAt(idx + 1) : ' ';
        if (n == '=') return "" + c + n;
        return "" + c;
    }

    private boolean compare(double cur, String op, double val) {
        return switch (op) {
            case ">=" -> cur >= val;
            case "<=" -> cur <= val;
            case ">" -> cur > val;
            case "<" -> cur < val;
            case "==" -> cur == val;
            case "!=" -> cur != val;
            default -> false;
        };
    }

    /* ───────────────── 액션 실행 ───────────────── */
    private void executeAction(Player p, RebornNpc npc, String action) {
        try {
            if (action.startsWith("addFavor:")) {
                double v = Double.parseDouble(action.substring(9));
                if (npc != null) npc.relations.addPlayer(p.getUniqueId(), v);
            } else if (action.startsWith("addEmotion:")) {
                String[] sp = action.substring("addEmotion:".length()).split(":");
                if (npc != null) {
                    Emotion.Kind k = Emotion.Kind.valueOf(sp[0].toUpperCase());
                    npc.emotion.add(k, Double.parseDouble(sp[1]));
                }
            } else if (action.startsWith("giveItem:")) {
                String[] sp = action.substring("giveItem:".length()).split(":");
                org.bukkit.Material mat = org.bukkit.Material.matchMaterial(sp[0]);
                int amt = sp.length >= 2 ? Integer.parseInt(sp[1]) : 1;
                if (mat != null) p.getInventory().addItem(new org.bukkit.inventory.ItemStack(mat, amt));
            } else if (action.startsWith("takeItem:")) {
                String[] sp = action.substring("takeItem:".length()).split(":");
                org.bukkit.Material mat = org.bukkit.Material.matchMaterial(sp[0]);
                int amt = sp.length >= 2 ? Integer.parseInt(sp[1]) : 1;
                if (mat != null) p.getInventory().removeItem(new org.bukkit.inventory.ItemStack(mat, amt));
            } else if (action.startsWith("giveQuest:")) {
                String qid = action.substring("giveQuest:".length());
                var qp = Bukkit.getPluginManager().getPlugin("RebornQuest");
                if (qp != null) {
                    Object engine = qp.getClass().getMethod("engine").invoke(qp);
                    engine.getClass().getMethod("accept", Player.class, String.class)
                            .invoke(engine, p, qid);
                }
            } else if (action.startsWith("addMemory:")) {
                String[] sp = action.substring("addMemory:".length()).split(":", 3);
                if (npc != null && npc.soul != null) {
                    Memory.Kind k = Memory.Kind.valueOf(sp[0].toUpperCase());
                    int intensity = sp.length >= 2 ? Integer.parseInt(sp[1]) : 5;
                    String label = sp.length >= 3 ? sp[2] : "대화";
                    npc.soul.memory.record(p.getUniqueId().toString(), k, intensity, label);
                }
            } else if (action.startsWith("stat:")) {
                String[] sp = action.substring("stat:".length()).split(":");
                kr.reborn.core.data.StatType st =
                        kr.reborn.core.data.StatType.valueOf(sp[0].toUpperCase());
                double v = Double.parseDouble(sp[1].replace("+", ""));
                RebornCore.get().api().addStat(p.getUniqueId(), st, v, "dialog");
            } else if (action.startsWith("broadcast:")) {
                Bukkit.broadcastMessage(action.substring("broadcast:".length()));
            } else if ("endDialog".equals(action)) {
                end(p);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Dialog action 실패 [" + action + "]: " + t.getMessage());
        }
    }
}
