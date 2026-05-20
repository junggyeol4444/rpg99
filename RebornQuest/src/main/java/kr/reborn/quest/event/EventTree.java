package kr.reborn.quest.event;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Rand;
import kr.reborn.quest.RebornQuest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 이벤트 트리 — 분기형 월드 이벤트.
 *
 * 인약사 암거래 (선계):
 *   FIND → INVESTIGATE → CONFRONT → 분기 (정의 / 합류 / 독자)
 *
 * 백귀야행 (요계):
 *   PREPARE → MARCH → SPECIAL_EVENT → ENDING
 *
 * 각 노드는 NPC 대화 또는 자동 트리거로 진행.
 */
public final class EventTree {

    private final RebornQuest plugin;

    /** playerUUID → "treeId:nodeId" */
    private final Map<java.util.UUID, String> playerProgress = new ConcurrentHashMap<>();

    public EventTree(RebornQuest plugin) { this.plugin = plugin; }

    public void start(Player p, String treeId) {
        EventTreeDef def = TREES.get(treeId);
        if (def == null) return;
        playerProgress.put(p.getUniqueId(), treeId + ":" + def.startNode);
        EventNode start = def.nodes.get(def.startNode);
        narrate(p, start);
    }

    public void choose(Player p, int branch) {
        String key = playerProgress.get(p.getUniqueId());
        if (key == null) return;
        String[] parts = key.split(":");
        EventTreeDef def = TREES.get(parts[0]);
        EventNode current = def.nodes.get(parts[1]);
        if (branch < 0 || branch >= current.choices.size()) return;
        String nextId = current.choices.get(branch).next;
        applyEffects(p, current.choices.get(branch).effects);
        if (nextId == null || nextId.isEmpty()) {
            playerProgress.remove(p.getUniqueId());
            kr.reborn.core.util.Msg.send(p, "&7이벤트 종료.");
            return;
        }
        EventNode next = def.nodes.get(nextId);
        playerProgress.put(p.getUniqueId(), parts[0] + ":" + nextId);
        narrate(p, next);
    }

    private void narrate(Player p, EventNode node) {
        kr.reborn.core.util.Msg.send(p, "&5&l[" + node.title + "]");
        for (String line : node.text) p.sendMessage("§7" + line);
        for (int i = 0; i < node.choices.size(); i++) {
            p.sendMessage("§e" + (i + 1) + ". §f" + node.choices.get(i).text + " §8(/eventchoice " + i + ")");
        }
    }

    private void applyEffects(Player p, List<String> effects) {
        for (String e : effects) {
            String[] parts = e.split(":", 2);
            switch (parts[0].toUpperCase()) {
                case "STAT":
                    String[] sa = parts[1].split("=");
                    try {
                        kr.reborn.core.data.StatType st = kr.reborn.core.data.StatType.valueOf(sa[0]);
                        RebornCore.get().api().addStat(p.getUniqueId(), st, Double.parseDouble(sa[1]), "event");
                    } catch (Exception ignored) {}
                    break;
                case "TITLE":
                    Bukkit.broadcastMessage("§6[칭호] §f" + p.getName() + " — " + parts[1]);
                    break;
                case "CURSE":
                    p.sendMessage("§4[저주 부여] §f" + parts[1]);
                    break;
                case "BLESSING":
                    p.sendMessage("§b[축복 부여] §f" + parts[1]);
                    break;
                case "BROADCAST":
                    Bukkit.broadcastMessage(parts[1]);
                    break;
            }
        }
    }

    // ============================================================
    // 백귀야행 자동 진행 (요계 보름달 트리거)
    // ============================================================
    public void triggerHundredDemonNight(WorldKey world) {
        if (world != WorldKey.YOKAI) return;
        Bukkit.broadcastMessage("§5§l[백귀야행] §f보름달 행렬이 시작된다!");
        // 5구간 — 60초 간격
        String[] segments = {
            "§51구간 §f야시 (요괴 상인 좌판)",
            "§52구간 §f무예 (비무 대회)",
            "§53구간 §f환술 (구미호 궁 경연)",
            "§54구간 §f귀문 (명계 기운, 강시 군단 경비)",
            "§55구간 §f만요봉 입장 (대요괴 연회)"
        };
        for (int i = 0; i < segments.length; i++) {
            final int idx = i;
            RebornCore.get().scheduler().runTaskLater(() -> {
                Bukkit.broadcastMessage("§5§l[백귀야행] " + segments[idx]);
                // 특수 이벤트 확률
                if (Rand.chance(0.15)) {
                    String[] specials = {
                        "§c§l[특수] 타세계 침입자 출현! 백귀야행 대란!",
                        "§e§l[특수] 요왕 도전 가능 — 행렬 중 대요괴에게 도전!",
                        "§5§l[특수] 달빛 폭주 — 요괴 광폭화. 정신력 높은 자만 정상 활동.",
                        "§6§l[특수] 숨겨진 보물 발견 — 탐색형 미니 퀘스트!"
                    };
                    Bukkit.broadcastMessage(specials[Rand.range(0, specials.length - 1)]);
                }
            }, 1200L * i);
        }
    }

    // ============================================================
    // 이벤트 트리 정의
    // ============================================================
    private static final Map<String, EventTreeDef> TREES = new java.util.HashMap<>();
    static {
        // 인약사 암거래 트리
        EventTreeDef inyak = new EventTreeDef("human_alchemist_trade", "find");
        inyak.nodes.put("find", new EventNode("수상한 행상인",
            List.of("선궁 도시 외곽에서 정체불명의 단약을 파는 자를 발견.",
                    "그의 단약에서 인간 영혼의 잔향이 느껴진다."),
            List.of(
                new EventChoice("조사한다", "investigate", List.of("STAT:MENTAL=2")),
                new EventChoice("무시한다", "", List.of()))));
        inyak.nodes.put("investigate", new EventNode("혼돈방 잠입",
            List.of("암시장 혼돈방에 잠입. 인약사 네트워크 단서 수집.",
                    "내부 협력자가 태허선궁에 있다는 정보 발견."),
            List.of(
                new EventChoice("계속 추적", "confront", List.of("STAT:INTELLIGENCE=3")),
                new EventChoice("물러난다", "", List.of()))));
        inyak.nodes.put("confront", new EventNode("대립",
            List.of("인약사 조직과 대면. 너의 선택은?"),
            List.of(
                new EventChoice("정의 — 태허선궁에 정보를 넘긴다",
                    "ending_justice",
                    List.of("STAT:MENTAL=20", "STAT:CHARISMA=10",
                            "TITLE:정의의 집행자",
                            "BROADCAST:§e§l[" + "EVENT" + "] §f정의가 인약사 조직을 토벌했다.")),
                new EventChoice("암거래 합류 — 이익을 취한다",
                    "ending_corrupt",
                    List.of("STAT:IMMORTAL_KI=300",
                            "CURSE:현상수배",
                            "BROADCAST:§4§l[현상수배] §f인약사 조직에 합류한 자가 있다.")),
                new EventChoice("독자 노선 — 대체 재료 연구",
                    "ending_neutral",
                    List.of("STAT:INTELLIGENCE=15",
                            "BLESSING:고유 단약 제조법")))));
        inyak.nodes.put("ending_justice", new EventNode("정의의 길",
            List.of("태허선궁의 명성과 함께 토벌이 시작된다.",
                    "인약사 조직은 흩어졌으나 뿌리는 깊다."), List.of()));
        inyak.nodes.put("ending_corrupt", new EventNode("어둠의 길",
            List.of("너의 선기가 짙어졌다. 그러나 추적자가 너를 노린다."), List.of()));
        inyak.nodes.put("ending_neutral", new EventNode("제3의 길",
            List.of("너만의 단약 제조법을 발견했다."), List.of()));
        TREES.put("human_alchemist_trade", inyak);
    }

    public static final class EventTreeDef {
        public final String id;
        public final String startNode;
        public final Map<String, EventNode> nodes = new java.util.HashMap<>();
        public EventTreeDef(String id, String start) { this.id = id; this.startNode = start; }
    }

    public static final class EventNode {
        public final String title;
        public final List<String> text;
        public final List<EventChoice> choices;
        public EventNode(String title, List<String> text, List<EventChoice> choices) {
            this.title = title; this.text = text; this.choices = choices;
        }
    }

    public static final class EventChoice {
        public final String text;
        public final String next;
        public final List<String> effects;
        public EventChoice(String text, String next, List<String> effects) {
            this.text = text; this.next = next; this.effects = effects;
        }
    }
}
