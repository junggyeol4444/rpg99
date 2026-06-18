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
        if (def == null) { playerProgress.remove(p.getUniqueId()); return; }
        EventNode current = def.nodes.get(parts[1]);
        if (current == null) { playerProgress.remove(p.getUniqueId()); return; }
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
                    // 실제 칭호 부여 (RebornTitle 리플렉션) — 이전엔 broadcast만 하고 미부여
                    try {
                        var tp = Bukkit.getPluginManager().getPlugin("RebornTitle");
                        if (tp != null) {
                            Object tm = tp.getClass().getMethod("titles").invoke(tp);
                            tm.getClass().getMethod("grant", Player.class, String.class)
                                    .invoke(tm, p, parts[1]);
                        }
                    } catch (Throwable ignored) {}
                    Bukkit.broadcastMessage("§6[칭호] §f" + p.getName() + " — " + parts[1]);
                    break;
                case "CURSE":
                    // 실제 저주 적용 (RebornCurse 리플렉션) — 이전엔 메시지만
                    try {
                        var cp = Bukkit.getPluginManager().getPlugin("RebornCurse");
                        if (cp != null) {
                            Object eff = cp.getClass().getMethod("effects").invoke(cp);
                            eff.getClass().getMethod("apply", Player.class, String.class)
                                    .invoke(eff, p, parts[1]);
                        }
                    } catch (Throwable ignored) {}
                    p.sendMessage("§4[저주 부여] §f" + parts[1]);
                    break;
                case "BLESSING":
                    try {
                        var cp = Bukkit.getPluginManager().getPlugin("RebornCurse");
                        if (cp != null) {
                            Object eff = cp.getClass().getMethod("effects").invoke(cp);
                            eff.getClass().getMethod("apply", Player.class, String.class)
                                    .invoke(eff, p, parts[1]);
                        }
                    } catch (Throwable ignored) {}
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

        // ============================================================
        // 무림 정사대전 (무협) — 정파/사파/마교 3분기 + 후속
        // ============================================================
        EventTreeDef wulin = new EventTreeDef("wulin_war", "choose_path");
        wulin.nodes.put("choose_path", new EventNode("정사대전 발발",
            List.of("정파의 무림맹과 사파, 마교가 충돌한다.",
                    "강호의 운명이 손에 달렸다 — 어느 길을 갈 것인가?"),
            List.of(
                new EventChoice("정파 — 무림맹 휘하",
                    "orthodox_join", List.of("STAT:CHARISMA=30", "STAT:INNER_KI=100", "TITLE:정파 후기지수")),
                new EventChoice("사파 — 자유의 길",
                    "sapa_join", List.of("STAT:AGILITY=30", "STAT:LUCK=20", "TITLE:사파 거두")),
                new EventChoice("마교 — 천마의 뜻",
                    "cult_join", List.of("STAT:DEMON_KI=200", "STAT:STRENGTH=30", "TITLE:마교 호법", "CURSE:bloodlust")))));
        wulin.nodes.put("orthodox_join", new EventNode("정파 일원",
            List.of("정파 일원으로서 사파·마교 토벌에 나선다.",
                    "첫 임무 — 마교 지부 단신 침투 또는 정파 동료 호위?"),
            List.of(
                new EventChoice("단신 침투 (영광)",
                    "orthodox_solo", List.of("STAT:STRENGTH=50", "BROADCAST:§3§l[정사대전] §f%player%이(가) 단신 침투 임무 수행")),
                new EventChoice("동료 호위 (안전)",
                    "orthodox_team", List.of("STAT:ENDURANCE=30", "STAT:CHARM=10")))));
        wulin.nodes.put("sapa_join", new EventNode("사파 거두",
            List.of("사파에서는 룰이 없다 — 누가 더 강한가만 중요.",
                    "마교의 비급을 약탈할 것인가? 정파의 영지를 칠 것인가?"),
            List.of(
                new EventChoice("마교 비급 강탈",
                    "sapa_loot_cult", List.of("STAT:INNER_KI=300", "STAT:LUCK=30")),
                new EventChoice("정파 영지 침공",
                    "sapa_attack_orthodox", List.of("STAT:STRENGTH=80")))));
        wulin.nodes.put("cult_join", new EventNode("마교 호법",
            List.of("천마의 명을 받들어 강호를 피로 물들인다."), List.of()));
        wulin.nodes.put("orthodox_solo", new EventNode("정파 영웅",
            List.of("단신 침투 성공 — 무림맹 정식 호법으로 승격."), List.of()));
        wulin.nodes.put("orthodox_team", new EventNode("정파 동료",
            List.of("호위 성공 — 정파 인맥 +."), List.of()));
        wulin.nodes.put("sapa_loot_cult", new EventNode("사파 거두",
            List.of("비급 강탈 — 마교가 추적한다. 살인청부 명단에 올랐다."), List.of()));
        wulin.nodes.put("sapa_attack_orthodox", new EventNode("사파 거두",
            List.of("정파 영지 함락 — 무림맹과 영구 적대."), List.of()));
        TREES.put("wulin_war", wulin);

        // ============================================================
        // 신앙 위기 (천계) — 타락 천사 반란
        // ============================================================
        EventTreeDef faith = new EventTreeDef("heaven_faith_crisis", "encounter");
        faith.nodes.put("encounter", new EventNode("타락한 대천사",
            List.of("절친한 대천사가 어둠에 빠졌다.",
                    "그의 신앙은 무너졌고, 그는 너에게 도움을 청한다."),
            List.of(
                new EventChoice("구원한다 — 회개의 빛",
                    "redeem", List.of("STAT:DIVINITY=20", "STAT:HEAVEN_KI=200", "TITLE:구원자")),
                new EventChoice("처단한다 — 타락은 용서 없다",
                    "execute", List.of("STAT:CHARISMA=20", "BROADCAST:§e§l[천계] §f%player%이(가) 타락 천사를 처단했다")),
                new EventChoice("함께 타락한다 — 천계의 한계",
                    "fall", List.of("STAT:DEMON_KI=300", "TITLE:타락자", "CURSE:monk_oath_break")))));
        faith.nodes.put("redeem", new EventNode("구원자",
            List.of("타락 천사가 빛의 길로 돌아왔다 — 천계의 칭송."), List.of()));
        faith.nodes.put("execute", new EventNode("정의 집행",
            List.of("냉정한 판결 — 천계의 질서는 지켜졌으나 마음은 무겁다."), List.of()));
        faith.nodes.put("fall", new EventNode("타락",
            List.of("천계를 등지고 어둠과 손잡았다.",
                    "이제 너의 적은 신이다."), List.of()));
        TREES.put("heaven_faith_crisis", faith);

        // ============================================================
        // 게이트 폭주 (지구) — S랭크 게이트 동시다발
        // ============================================================
        EventTreeDef gate = new EventTreeDef("gate_overflow", "alarm");
        gate.nodes.put("alarm", new EventNode("긴급 경보",
            List.of("S랭크 게이트가 4개 동시 출현.",
                    "헌터 협회는 너에게 1개 담당을 요청한다."),
            List.of(
                new EventChoice("서울 강남 (드래곤)",
                    "gangnam", List.of("STAT:STRENGTH=30", "STAT:ENDURANCE=30", "TITLE:용 사냥꾼")),
                new EventChoice("부산 해운대 (해마)",
                    "haeundae", List.of("STAT:AGILITY=30", "STAT:OCEAN_POWER=50")),
                new EventChoice("제주 한라산 (정령왕)",
                    "halla", List.of("STAT:SPIRIT_POWER=50", "STAT:MENTAL=20")),
                new EventChoice("백두산 (마왕)",
                    "baekdu", List.of("STAT:DEMON_KI=100", "STAT:CHARISMA=30", "BROADCAST:§4§l[게이트] §f%player%이(가) 백두산 마왕 게이트 진입")))));
        gate.nodes.put("gangnam", new EventNode("강남 드래곤",
            List.of("초고층 빌딩들 사이에서 드래곤과의 사투. 강남 시민 1만명 구조."), List.of()));
        gate.nodes.put("haeundae", new EventNode("해운대 해마",
            List.of("해변의 거대 해마 — 바다 위 결투."), List.of()));
        gate.nodes.put("halla", new EventNode("한라 정령왕",
            List.of("한라산 정상에서 정령왕과의 영적 결투."), List.of()));
        gate.nodes.put("baekdu", new EventNode("백두 마왕",
            List.of("백두산 호수가 검게 변했다 — 마왕 직접 강림."), List.of()));
        TREES.put("gate_overflow", gate);

        // ============================================================
        // 해양 패권 (해양) — 해적왕 후계자 결정전
        // ============================================================
        EventTreeDef sea = new EventTreeDef("sea_succession", "competition");
        sea.nodes.put("competition", new EventNode("해적왕 후계자전",
            List.of("죽은 해적왕의 자리를 두고 7대 해적단이 패권 다툼.",
                    "어느 해적단에 합류할 것인가?"),
            List.of(
                new EventChoice("백패 해적단 (정의)",
                    "white", List.of("STAT:CHARISMA=30", "STAT:OCEAN_POWER=100", "TITLE:백패 선장")),
                new EventChoice("검사단 (전투광)",
                    "black", List.of("STAT:STRENGTH=50", "STAT:AGILITY=30", "TITLE:검사단 일원")),
                new EventChoice("암해단 (어둠)",
                    "shadow", List.of("STAT:AGILITY=50", "STAT:LUCK=30", "BLESSING:pirate_curse")))));
        sea.nodes.put("white", new EventNode("백패 선장",
            List.of("정의의 해적 — 약자 보호 + 보물 분배."), List.of()));
        sea.nodes.put("black", new EventNode("검사단",
            List.of("최강의 해적 — 다른 해적단과 모두 적대."), List.of()));
        sea.nodes.put("shadow", new EventNode("암해단",
            List.of("그림자에서 활동 — 해적 저주를 받아들였다."), List.of()));
        TREES.put("sea_succession", sea);

        // ============================================================
        // 폐허 생존 (아포칼립스) — 정착지 운명 결정
        // ============================================================
        EventTreeDef apoc = new EventTreeDef("apoc_settlement", "arrival");
        apoc.nodes.put("arrival", new EventNode("정착지 도착",
            List.of("폐허 속 유일한 인간 정착지에 도착했다.",
                    "정착지는 약탈단의 공격 위기에 놓여 있다."),
            List.of(
                new EventChoice("방어 — 약탈단과 싸운다",
                    "defend", List.of("STAT:STRENGTH=30", "STAT:ENDURANCE=30", "TITLE:정착지 수호자")),
                new EventChoice("협상 — 식량을 바친다",
                    "negotiate", List.of("STAT:CHARISMA=20", "STAT:INTELLIGENCE=20")),
                new EventChoice("배신 — 약탈단에 가담",
                    "betray", List.of("STAT:LUCK=30", "BROADCAST:§4§l[폐허] §f%player%이(가) 정착지를 배신했다")))));
        apoc.nodes.put("defend", new EventNode("정착지 수호자",
            List.of("정착지를 지켜냈다 — 생존자들의 영웅."), List.of()));
        apoc.nodes.put("negotiate", new EventNode("외교가",
            List.of("협상 성공 — 평화를 샀으나 식량 위기는 계속."), List.of()));
        apoc.nodes.put("betray", new EventNode("배신자",
            List.of("정착지가 약탈당했다 — 약탈단의 일원으로 살아간다."), List.of()));
        TREES.put("apoc_settlement", apoc);

        // ============================================================
        // 마계 신마전쟁 — 천계 vs 마계 전면전 (마계 시점)
        // ============================================================
        EventTreeDef sinmaWar = new EventTreeDef("demon_world_invasion", "summons");
        sinmaWar.nodes.put("summons", new EventNode("마왕의 소집",
            List.of("7대 마왕이 동시에 외친다. 천계가 토벌 선언했다.",
                    "지원군이 필요하다. 너는 어느 진영에 설 것인가?"),
            List.of(
                new EventChoice("루시페르 군에 합류 (교만)", "lucifer_path", List.of("STAT:DEMON_KI=200")),
                new EventChoice("사탄 군에 합류 (분노)", "satan_path", List.of("STAT:STRENGTH=30")),
                new EventChoice("중립 — 마몬과 거래", "mammon_path", List.of("STAT:CHARISMA=20")))));
        sinmaWar.nodes.put("lucifer_path", new EventNode("교만의 군단",
            List.of("타락 천사 군단 선봉으로 무간지대 진격."),
            List.of(
                new EventChoice("천계 본진 공격", "ending_assault", List.of("STAT:DEMON_KI=500", "TITLE:천계 도전자")),
                new EventChoice("배신 — 천계 측에 정보", "ending_traitor", List.of("CURSE:마계 추방자")))));
        sinmaWar.nodes.put("satan_path", new EventNode("분노의 전장",
            List.of("끝없는 살육. 살의가 곧 힘."),
            List.of(
                new EventChoice("학살자가 된다", "ending_carnage", List.of("STAT:STRENGTH=80", "STAT:DEMON_KI=400", "CURSE:살육광")),
                new EventChoice("전선 이탈", "", List.of()))));
        sinmaWar.nodes.put("mammon_path", new EventNode("탐욕의 거래",
            List.of("양측 모두에게 무기를 팔며 부를 축적."),
            List.of(
                new EventChoice("양측 거래 성공", "ending_merchant", List.of("STAT:CHARISMA=50", "TITLE:차원 상인")))));
        sinmaWar.nodes.put("ending_assault", new EventNode("천계 침공",
            List.of("무간지대에서 격전 — 너의 이름이 양 세계에 알려진다."), List.of()));
        sinmaWar.nodes.put("ending_traitor", new EventNode("배신자",
            List.of("천계가 너를 받아들였으나, 마계는 영원한 적이 됐다."), List.of()));
        sinmaWar.nodes.put("ending_carnage", new EventNode("학살의 화신",
            List.of("너의 살의가 살의를 부른다. 영원한 분노 속에 산다."), List.of()));
        sinmaWar.nodes.put("ending_merchant", new EventNode("차원 상인",
            List.of("양 진영 모두 너에게 빚을 졌다. 부와 영향력이 너의 무기."), List.of()));
        TREES.put("demon_world_invasion", sinmaWar);

        // ============================================================
        // 천계 신마대전 — 천계 시점의 신마전쟁
        // ============================================================
        EventTreeDef heavenWar = new EventTreeDef("heaven_divine_war", "muster");
        heavenWar.nodes.put("muster", new EventNode("천사 군단 소집",
            List.of("4대천사 미카엘이 호출한다. 마계의 도전을 받는다.",
                    "너의 위치는?"),
            List.of(
                new EventChoice("선봉대 (전투)", "vanguard", List.of("STAT:STRENGTH=20", "STAT:HEAVEN_KI=200")),
                new EventChoice("정화단 (치유)", "purify", List.of("STAT:DIVINITY=10", "STAT:MENTAL=20")),
                new EventChoice("정보국 (간첩)", "spy", List.of("STAT:INTELLIGENCE=20", "STAT:LUCK=10")))));
        heavenWar.nodes.put("vanguard", new EventNode("최전선",
            List.of("발록 군단과 격돌. 살아남는 자만이 명예를 안는다."),
            List.of(
                new EventChoice("발록 처치 시도", "ending_balrog", List.of("STAT:HEAVEN_KI=500", "TITLE:발록 도살자")),
                new EventChoice("후퇴", "", List.of("STAT:MENTAL=-10")))));
        heavenWar.nodes.put("purify", new EventNode("성지 정화",
            List.of("타락한 천사의 영혼을 정화. 시간이 약."),
            List.of(
                new EventChoice("정화 완성", "ending_pure", List.of("STAT:DIVINITY=50", "TITLE:정화의 손길")))));
        heavenWar.nodes.put("spy", new EventNode("마계 잠입",
            List.of("적의 본진에 침투. 정보를 가져온다."),
            List.of(
                new EventChoice("정보 회수", "ending_spy", List.of("STAT:INTELLIGENCE=40", "TITLE:천사 간첩")),
                new EventChoice("배신 — 마계에 잔류", "ending_fall", List.of("CURSE:타락 천사")))));
        heavenWar.nodes.put("ending_balrog", new EventNode("발록 도살자",
            List.of("너의 이름이 천사 군단의 노래에 새겨진다."), List.of()));
        heavenWar.nodes.put("ending_pure", new EventNode("정화의 사도",
            List.of("천사들 사이 존경의 대상. 정화의 손길."), List.of()));
        heavenWar.nodes.put("ending_spy", new EventNode("천사 간첩",
            List.of("그림자 속 영웅. 천계 정보국 핵심."), List.of()));
        heavenWar.nodes.put("ending_fall", new EventNode("타락",
            List.of("천계가 너를 추방했다. 너는 이제 타락 천사다."), List.of()));
        TREES.put("heaven_divine_war", heavenWar);

        // ============================================================
        // 무협 천마 강림 — 천마가 부활하여 무림 전체 위협
        // ============================================================
        EventTreeDef tianmo = new EventTreeDef("demon_descent", "omen");
        tianmo.nodes.put("omen", new EventNode("불길한 징조",
            List.of("천하 곳곳에서 검은 안개. 천마의 잔영이 부활을 예고한다.",
                    "너의 입장은?"),
            List.of(
                new EventChoice("정파 연합에 합류", "right_ally", List.of("STAT:INNER_KI=100", "STAT:MENTAL=20")),
                new EventChoice("천마 측 — 마교 교주에 도전", "left_ally", List.of("STAT:DEMON_KI=200")),
                new EventChoice("관망 — 중립", "neutral", List.of("STAT:INTELLIGENCE=20")))));
        tianmo.nodes.put("right_ally", new EventNode("정파 연합",
            List.of("소림·무당·화산 연합군. 너는 그 일원."),
            List.of(
                new EventChoice("최전선 출진", "ending_hero", List.of("STAT:INNER_KI=500", "TITLE:정파 영웅")),
                new EventChoice("후방 지원", "", List.of("STAT:CHARISMA=20")))));
        tianmo.nodes.put("left_ally", new EventNode("천마 측",
            List.of("천마의 힘은 인간을 초월. 너 또한 그 길을 갈 것인가?"),
            List.of(
                new EventChoice("천마신공 수련", "ending_demon", List.of("STAT:DEMON_KI=800", "STAT:INNER_KI=400", "TITLE:천마 후계자")),
                new EventChoice("천마에 배신 — 정파로", "ending_betray", List.of("TITLE:이중첩자")))));
        tianmo.nodes.put("neutral", new EventNode("관망자",
            List.of("힘의 균형을 본다. 누구 편이 될지 결정하지 않았다."),
            List.of(
                new EventChoice("기회를 노린다", "ending_opportunist", List.of("STAT:LUCK=30", "TITLE:기회주의자")))));
        tianmo.nodes.put("ending_hero", new EventNode("정파 영웅",
            List.of("천마와 결전. 영광이 너의 것."), List.of()));
        tianmo.nodes.put("ending_demon", new EventNode("천마 후계자",
            List.of("너 안의 천마가 깨어났다. 무림이 너를 두려워한다."), List.of()));
        tianmo.nodes.put("ending_betray", new EventNode("이중첩자",
            List.of("양측 정보를 손에 쥐고 살아남았다."), List.of()));
        tianmo.nodes.put("ending_opportunist", new EventNode("기회주의자",
            List.of("결전 후 보물을 손에 넣었다."), List.of()));
        TREES.put("demon_descent", tianmo);

        // ============================================================
        // 선계 천겁 이변 — 천겁뇌해가 비정상 확장
        // ============================================================
        EventTreeDef cheonGeop = new EventTreeDef("heavenly_chaos", "warning");
        cheonGeop.nodes.put("warning", new EventNode("천겁 이변",
            List.of("천겁뇌해가 36동천 영역을 침식한다. 태허선궁이 행동을 요구한다."),
            List.of(
                new EventChoice("뇌전 흡수 시도 (위험)", "absorb", List.of("STAT:LUCK=10")),
                new EventChoice("동천 방어 (안전)", "defend", List.of("STAT:MENTAL=20")),
                new EventChoice("도망 — 다른 세계로", "flee", List.of("STAT:AGILITY=20")))));
        cheonGeop.nodes.put("absorb", new EventNode("뇌전 흡수",
            List.of("천겁뇌해의 뇌전을 직접 흡수. 생존 시 보상은 거대."),
            List.of(
                new EventChoice("계속 흡수", "ending_lightning", List.of("STAT:IMMORTAL_KI=1000", "TITLE:뇌전의 선인")),
                new EventChoice("중단 — 안전 우선", "", List.of("STAT:MENTAL=-30")))));
        cheonGeop.nodes.put("defend", new EventNode("동천 방어",
            List.of("36동천 중 일부가 너의 방어로 보존됐다."),
            List.of(
                new EventChoice("계속 방어", "ending_guardian", List.of("STAT:ENDURANCE=50", "TITLE:36동천 수호자")))));
        cheonGeop.nodes.put("flee", new EventNode("도망자",
            List.of("선계를 떠나 다른 차원으로 피신."),
            List.of(
                new EventChoice("적응", "ending_exile", List.of("STAT:AGILITY=30", "CURSE:선계 추방")))));
        cheonGeop.nodes.put("ending_lightning", new EventNode("뇌전의 선인",
            List.of("너의 몸에 뇌전이 깃들었다. 한 발자국이 천둥."), List.of()));
        cheonGeop.nodes.put("ending_guardian", new EventNode("36동천 수호자",
            List.of("태허선궁이 너를 인정. 선계의 보루."), List.of()));
        cheonGeop.nodes.put("ending_exile", new EventNode("선계 추방자",
            List.of("선계 문이 닫혔다. 너는 이제 떠도는 자."), List.of()));
        TREES.put("heavenly_chaos", cheonGeop);

        // ============================================================
        // 지구 미궁 100층 정복
        // ============================================================
        EventTreeDef labyrinth = new EventTreeDef("maze_100_conquest", "summons");
        labyrinth.nodes.put("summons", new EventNode("100층 도전",
            List.of("미궁 100층의 문이 모든 헌터에게 열렸다.",
                    "각성한 자만이 도전 가능."),
            List.of(
                new EventChoice("단독 도전 (위험)", "solo", List.of("STAT:LUCK=10")),
                new EventChoice("길드 레이드 합류", "raid", List.of("STAT:CHARISMA=20")),
                new EventChoice("정보만 분석", "analyze", List.of("STAT:INTELLIGENCE=30")))));
        labyrinth.nodes.put("solo", new EventNode("단독 도전",
            List.of("100층 보스 — 미궁의 핵심."),
            List.of(
                new EventChoice("핵심 격파", "ending_solo_clear", List.of("STAT:LEVEL=10", "TITLE:단독 정복자")),
                new EventChoice("후퇴", "", List.of("STAT:MENTAL=-20")))));
        labyrinth.nodes.put("raid", new EventNode("길드 레이드",
            List.of("S랭크 길드와 100명 동시 도전."),
            List.of(
                new EventChoice("MVP 등극", "ending_raid_mvp", List.of("STAT:LEVEL=8", "STAT:CHARISMA=30", "TITLE:레이드 MVP")),
                new EventChoice("팀워크 우선", "ending_raid_team", List.of("STAT:LEVEL=5", "STAT:CHARISMA=20")))));
        labyrinth.nodes.put("analyze", new EventNode("분석가",
            List.of("미궁 구조를 해독. 모든 헌터 협회의 자료가 너의 손에."),
            List.of(
                new EventChoice("정보 판매", "ending_analyst", List.of("STAT:INTELLIGENCE=50", "TITLE:미궁 학자")))));
        labyrinth.nodes.put("ending_solo_clear", new EventNode("단독 정복자",
            List.of("미궁의 의지가 너를 인정. 세계가 바뀐다."), List.of()));
        labyrinth.nodes.put("ending_raid_mvp", new EventNode("레이드 MVP",
            List.of("100층 정복의 영웅. 모든 길드가 너를 원한다."), List.of()));
        labyrinth.nodes.put("ending_raid_team", new EventNode("팀의 일원",
            List.of("정복은 함께. 보상도 함께."), List.of()));
        labyrinth.nodes.put("ending_analyst", new EventNode("미궁 학자",
            List.of("이제 모든 헌터가 너를 찾는다."), List.of()));
        TREES.put("maze_100_conquest", labyrinth);

        // ============================================================
        // 마도공학 마도 폭주
        // ============================================================
        EventTreeDef mado = new EventTreeDef("mado_runaway", "siren");
        mado.nodes.put("siren", new EventNode("마도 사이렌",
            List.of("7대 도시 중 한 곳에서 마도 에너지 폭주.",
                    "너의 대응은?"),
            List.of(
                new EventChoice("진압 — 폭주 봉인", "suppress", List.of("STAT:INTELLIGENCE=30")),
                new EventChoice("흡수 — 위험하지만 큰 보상", "absorb", List.of("STAT:LUCK=20")),
                new EventChoice("연구 — 데이터 수집", "research", List.of("STAT:MAGITECH_ENERGY=100")))));
        mado.nodes.put("suppress", new EventNode("폭주 진압",
            List.of("도시 한 구역이 너의 손에 살아남았다."),
            List.of(
                new EventChoice("성공", "ending_savior", List.of("STAT:MAGITECH_ENERGY=300", "TITLE:도시 수호자")))));
        mado.nodes.put("absorb", new EventNode("에너지 흡수",
            List.of("폭주 에너지가 너의 몸을 갉아먹는다."),
            List.of(
                new EventChoice("계속 흡수", "ending_overload", List.of("STAT:MAGITECH_ENERGY=800", "CURSE:과부하 체질")),
                new EventChoice("중단", "", List.of()))));
        mado.nodes.put("research", new EventNode("연구자",
            List.of("폭주 데이터 수집. 학계의 영웅."),
            List.of(
                new EventChoice("논문 발표", "ending_researcher", List.of("STAT:INTELLIGENCE=50", "TITLE:마도 학자")))));
        mado.nodes.put("ending_savior", new EventNode("도시 수호자",
            List.of("도시민이 너를 영웅으로 기린다."), List.of()));
        mado.nodes.put("ending_overload", new EventNode("과부하의 화신",
            List.of("너의 몸이 마도 에너지로 가득. 일상이 위험하다."), List.of()));
        mado.nodes.put("ending_researcher", new EventNode("마도 학자",
            List.of("학계의 정점. 모든 연구소가 너를 환영."), List.of()));
        TREES.put("mado_runaway", mado);

        // ============================================================
        // 드래곤 용왕 부활
        // ============================================================
        EventTreeDef dragonRevival = new EventTreeDef("dragon_revival", "awakening");
        dragonRevival.nodes.put("awakening", new EventNode("용왕 부활의 징조",
            List.of("고대 용왕 아우렐리스 0세의 잠이 깨어난다.",
                    "5대 가문이 동시에 호출. 너의 진영은?"),
            List.of(
                new EventChoice("용왕에 충성", "loyalty", List.of("STAT:DRAGON_POWER=200")),
                new EventChoice("이그니페르 가문 — 반란", "rebellion", List.of("STAT:STRENGTH=30")),
                new EventChoice("녹테르나 — 그림자 속에서", "shadow", List.of("STAT:INTELLIGENCE=30")))));
        dragonRevival.nodes.put("loyalty", new EventNode("충성의 길",
            List.of("용왕의 부활 의식에 동참. 너의 충성이 보상받는다."),
            List.of(
                new EventChoice("의식 호위", "ending_loyal", List.of("STAT:DRAGON_POWER=600", "TITLE:용왕 호위")),
                new EventChoice("후방 지원", "", List.of()))));
        dragonRevival.nodes.put("rebellion", new EventNode("반란",
            List.of("이그니페르가 용왕에 도전. 천명을 빼앗으려 한다."),
            List.of(
                new EventChoice("전면 공격", "ending_rebel", List.of("STAT:STRENGTH=80", "STAT:DRAGON_POWER=500", "CURSE:반역자")),
                new EventChoice("배신 — 용왕 측에", "", List.of()))));
        dragonRevival.nodes.put("shadow", new EventNode("그림자",
            List.of("녹테르나 가문은 정보로 승부."),
            List.of(
                new EventChoice("간첩 활동", "ending_spy", List.of("STAT:INTELLIGENCE=60", "STAT:LUCK=30", "TITLE:용계 간첩")))));
        dragonRevival.nodes.put("ending_loyal", new EventNode("용왕 호위",
            List.of("용왕이 너를 그의 곁에 둔다. 천명 다음의 위치."), List.of()));
        dragonRevival.nodes.put("ending_rebel", new EventNode("반역자",
            List.of("용왕이 처형 영장을 발행했다. 도망쳐야 한다."), List.of()));
        dragonRevival.nodes.put("ending_spy", new EventNode("용계 간첩",
            List.of("모든 가문의 정보가 너의 손에. 진정한 권력자."), List.of()));
        TREES.put("dragon_revival", dragonRevival);
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
