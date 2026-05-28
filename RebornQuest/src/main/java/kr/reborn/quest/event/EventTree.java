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

        // ============================================================
        // 마왕령 접경 — 평시/전조/후속 (판타지)
        // ============================================================
        EventTreeDef border = new EventTreeDef("demon_border", "patrol");
        border.nodes.put("patrol", new EventNode("접경 순찰",
            List.of("아르테온·자유 도시 연합 합동 순찰대가 접경을 돈다.",
                    "마물 출몰 빈도에 따라 순찰 등급(녹/황/적)이 변동."),
            List.of(
                new EventChoice("순찰에 합류한다", "rift_watch", List.of("STAT:STRENGTH=3")),
                new EventChoice("마을 방어전을 돕는다", "village_def", List.of("STAT:ENDURANCE=3")),
                new EventChoice("그냥 지나간다", "", List.of()))));
        border.nodes.put("rift_watch", new EventNode("차원의 틈 감시",
            List.of("대마탑 마법사가 안정도를 측정 — 수치가 임계점 이하다.",
                    "전조: 마기 폭풍·데몬 정찰대 침입·모르간 밀사 목격."),
            List.of(
                new EventChoice("마기 폭풍을 잠재운다", "epilogue_seal",
                    List.of("STAT:MENTAL=10", "STAT:MANA=20", "BROADCAST:§b§l[접경] §f플레이어가 차원의 틈을 안정시켰다.")),
                new EventChoice("데몬 정찰대를 추격", "epilogue_chase",
                    List.of("STAT:STRENGTH=10", "TITLE:마왕령 추격자")),
                new EventChoice("모르간 밀사를 미행", "epilogue_intel",
                    List.of("STAT:LUCK=10", "BLESSING:흑마법 단서")))));
        border.nodes.put("village_def", new EventNode("접경 마을 방어전",
            List.of("소규모 마을이 마물 습격을 받는 중. 시간이 없다."),
            List.of(
                new EventChoice("정면으로 막아낸다", "epilogue_hero",
                    List.of("STAT:STRENGTH=15", "STAT:ENDURANCE=10", "TITLE:접경의 영웅")),
                new EventChoice("주민 대피를 우선", "epilogue_savior",
                    List.of("STAT:CHARISMA=10", "STAT:MENTAL=5", "TITLE:생명의 인도자")))));
        border.nodes.put("epilogue_seal", new EventNode("틈의 봉인",
            List.of("차원의 틈이 잠시 안정됐다. 마계와의 연결이 약해진다."), List.of()));
        border.nodes.put("epilogue_chase", new EventNode("추격의 끝",
            List.of("데몬 정찰대의 본대 위치를 파악했다. 다음 침공이 보인다."), List.of()));
        border.nodes.put("epilogue_intel", new EventNode("어둠의 흔적",
            List.of("모르간 공국과 마계의 거래 단서를 잡았다."), List.of()));
        border.nodes.put("epilogue_hero", new EventNode("마을의 수호자",
            List.of("마을이 살았다. 사람들이 너의 이름을 새긴다."), List.of()));
        border.nodes.put("epilogue_savior", new EventNode("생명의 인도자",
            List.of("주민들이 무사히 피난했다. 마을은 잿더미가 됐지만 사람은 산다."), List.of()));
        TREES.put("demon_border", border);

        // ============================================================
        // 마왕의 침공 — 진영 분기 A/B/C
        // ============================================================
        EventTreeDef marwang = new EventTreeDef("marwang_invasion_branches", "choose_side");
        marwang.nodes.put("choose_side", new EventNode("진영 선택",
            List.of("마왕군 본대가 남부 전선에 진격한다.",
                    "너는 어느 편에 설 것인가?"),
            List.of(
                new EventChoice("§4마왕 편 — 함께 중간계를 무너뜨린다",
                    "side_demon",
                    List.of("STAT:DEMON_KI=200", "TITLE:중간계 침략자",
                            "CURSE:마기 침식",
                            "BROADCAST:§4§l[배신] §f한 인간이 마왕군에 합류했다.")),
                new EventChoice("§b방어 편 — 연합군과 함께 싸운다",
                    "side_defender",
                    List.of("STAT:STRENGTH=100", "STAT:AURA=50", "TITLE:대전의 영웅",
                            "BROADCAST:§a§l[참전] §f연합군에 영웅이 합류했다.")),
                new EventChoice("§7제3세력 — 양쪽 모두에 거리를 둔다",
                    "side_third",
                    List.of("STAT:LUCK=30", "STAT:INTELLIGENCE=50",
                            "BLESSING:전장의 관찰자")))));
        marwang.nodes.put("side_demon", new EventNode("마왕군 부장",
            List.of("마왕군 본대의 부장으로 임명. 정복지 일부의 지배권을 받는다."), List.of()));
        marwang.nodes.put("side_defender", new EventNode("연합군 영웅",
            List.of("연합군의 핵심 전력으로 이름을 떨친다."), List.of()));
        marwang.nodes.put("side_third", new EventNode("암중행",
            List.of("혼란 속에서 너만의 길을 본다. 양 진영의 비밀이 너에게 모인다."), List.of()));
        TREES.put("marwang_invasion_branches", marwang);

        // ============================================================
        // 정령왕의 분노 — 진영 분기
        // ============================================================
        EventTreeDef spirit = new EventTreeDef("spirit_king_rage_branches", "stance");
        spirit.nodes.put("stance", new EventNode("정령왕의 분노",
            List.of("4대 정령왕 중 하나의 영역이 오염됐다.",
                    "야생 정령이 폭주하고 자연재해급 원소 폭풍이 인다."),
            List.of(
                new EventChoice("정령왕 편 — 오염 세력 토벌",
                    "side_purify",
                    List.of("STAT:SPIRIT_POWER=150", "TITLE:자연의 수호자",
                            "BLESSING:정령왕의 가호")),
                new EventChoice("오염 세력 편 — 정령왕에 대항",
                    "side_corrupt",
                    List.of("STAT:DEMON_KI=80", "CURSE:정령왕의 분노")),
                new EventChoice("중재 시도 — 외교로 해결",
                    "side_mediate",
                    List.of("STAT:CHARISMA=50", "STAT:MENTAL=30",
                            "TITLE:평화의 사자")))));
        spirit.nodes.put("side_purify", new EventNode("정화",
            List.of("정령왕이 직접 너에게 축복을 내린다."), List.of()));
        spirit.nodes.put("side_corrupt", new EventNode("오염의 길",
            List.of("정령왕의 분노를 사면서 어둠의 힘을 얻었다."), List.of()));
        spirit.nodes.put("side_mediate", new EventNode("중재자",
            List.of("양쪽이 잠시 멈춘다. 영구한 해법은 멀지만 피는 더 흐르지 않는다."), List.of()));
        TREES.put("spirit_king_rage_branches", spirit);

        // ============================================================
        // 기업 전쟁 — 메가코프 진영 (사이버펑크)
        // ============================================================
        EventTreeDef corp = new EventTreeDef("megacorp_war", "choose_corp");
        corp.nodes.put("choose_corp", new EventNode("메가코프 전쟁",
            List.of("7대 메가코프 간 전면전이 발발했다.",
                    "어느 진영의 전쟁병기를 끌어들일 것인가?"),
            List.of(
                new EventChoice("AKRO(인체개조) 합류",
                    "akro", List.of("STAT:CYBER_ADAPTATION=50", "TITLE:AKRO 임원"))
                , new EventChoice("JINTECH(AI) 합류",
                    "jin", List.of("STAT:INTELLIGENCE=50", "TITLE:JINTECH 임원"))
                , new EventChoice("리벨리온 합류 — 기업에 맞선다",
                    "rebel", List.of("STAT:AGILITY=50", "STAT:LUCK=30",
                                     "TITLE:혁명의 기수")))));
        corp.nodes.put("akro", new EventNode("AKRO 정예",
            List.of("강화된 신체로 전선에 선다. 인간이지만 더 이상 인간이 아니다."), List.of()));
        corp.nodes.put("jin", new EventNode("JINTECH 정예",
            List.of("AI 동맹군이 너의 명령을 따른다."), List.of()));
        corp.nodes.put("rebel", new EventNode("저항군",
            List.of("언더시티에서 봉기의 불꽃이 일어난다."), List.of()));
        TREES.put("megacorp_war", corp);
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
