package kr.reborn.npc.soul;

import kr.reborn.core.util.Rand;
import kr.reborn.npc.RebornNPC;
import kr.reborn.npc.entity.RebornNpc;

import java.util.ArrayList;
import java.util.List;

/**
 * NPC의 성격·욕구·기억·세계 상황에서 새 목표를 자동 생성.
 *
 * 매 사이클마다 활성 목표 슬롯에 빈자리가 있으면 새 목표 추가 시도.
 * 슬롯이 꽉 차도 더 가치 있는 목표가 생기면 우선순위 가장 낮은 것을 교체.
 *
 * 생성 규칙:
 *   - AMBITION 60+ + 세력 없음 → FOUND_TOWN
 *   - AMBITION 70+ + 세력 있음 → GAIN_POWER
 *   - AMBITION 80+ + 가문장 → ASCEND (왕국 → 마왕 → 신)
 *   - LOYALTY 70+ + 강한 NPC 있음 (자기 세력) → SERVE_LORD
 *   - GREED 70+ → GAIN_WEALTH / START_BUSINESS
 *   - LOVE 욕구 30 미만 + 미혼 → FIND_LOVE
 *   - 친구 살해 기억 → AVENGE (target = killer)
 *   - 가족 살해 기억 → AVENGE (priority 95, decay 없음)
 *   - 가족 다수 + 위협 감지 → PROTECT_FAMILY
 *   - CURIOSITY 70+ → EXPLORE / ACCUMULATE_KNOWLEDGE
 *   - PRIDE 70+ + 라이벌 있음 → DEFEAT_RIVAL
 *   - 직업 종교 + EMPATHY 50+ → FOUND_RELIGION
 *   - LOYALTY 매우 낮음 (-50 미만) + 충성 중인 세력 있음 → BETRAY
 *   - SOCIABILITY -50 미만 → HIDE
 */
public final class GoalGenerator {

    public static final int MAX_GOALS = 3;

    private final RebornNPC plugin;

    public GoalGenerator(RebornNPC plugin) { this.plugin = plugin; }

    /** 매 사이클 호출. 빈 슬롯에 새 목표 추가. */
    public void considerNewGoal(RebornNpc npc) {
        if (npc.soul == null || npc.dead) return;
        // 활성 목표 정리
        npc.goals.removeIf(g -> g.isFulfilled() || g.abandoned);
        if (npc.goals.size() >= MAX_GOALS) return;
        // 5% 확률로만 새 목표 검토 (NPC가 너무 자주 목표 갈아치우지 않도록)
        if (!Rand.chance(0.05)) return;

        Goal candidate = generate(npc);
        if (candidate == null) return;

        // 이미 같은 종류 목표가 있으면 추가 안 함
        for (Goal g : npc.goals) {
            if (g.kind == candidate.kind && g.target.equals(candidate.target)) return;
        }
        npc.goals.add(candidate);
    }

    /** 가장 적합한 목표 1개 반환. */
    public Goal generate(RebornNpc npc) {
        var p = npc.soul.personality;
        var n = npc.soul.needs;
        List<Goal> candidates = new ArrayList<>();

        // 복수 (강력 — 우선순위 90~100)
        for (Memory.Entry e : npc.soul.memory.all()) {
            if (e.kind == Memory.Kind.KILLED_MY_FAMILY && e.currentIntensity() > 50) {
                Goal g = new Goal(GoalKind.AVENGE, e.subject,
                        "가족을 죽인 " + shortenId(e.subject) + "에게 복수한다");
                g.priority = 100;
                candidates.add(g);
            } else if (e.kind == Memory.Kind.KILLED_MY_FRIEND && e.currentIntensity() > 40) {
                Goal g = new Goal(GoalKind.AVENGE, e.subject,
                        "친구를 죽인 " + shortenId(e.subject) + "에게 복수한다");
                g.priority = 90;
                candidates.add(g);
            }
        }

        // 권력 / 마을 / 초월
        int ambition = p.get(Personality.Trait.AMBITION);
        if (ambition >= 80 && hasClanRank(npc, "LEADER")) {
            Goal g = new Goal(GoalKind.ASCEND, "", "왕·마왕·신이 된다");
            g.priority = 85;
            candidates.add(g);
        } else if (ambition >= 70 && !npc.faction.isEmpty()) {
            Goal g = new Goal(GoalKind.GAIN_POWER, "",
                    "권력을 얻어 가문을 이끈다");
            g.priority = 70;
            candidates.add(g);
        } else if (ambition >= 60 && npc.faction.isEmpty()) {
            Goal g = new Goal(GoalKind.FOUND_TOWN, "",
                    "새 마을을 세워 내 사람들을 모은다");
            g.priority = 60;
            candidates.add(g);
        }

        // 충성 — 강한 자 섬김
        int loyalty = p.get(Personality.Trait.LOYALTY);
        if (loyalty >= 70 && !npc.faction.isEmpty()) {
            RebornNpc lord = findStrongestInFaction(npc);
            if (lord != null && lord != npc) {
                Goal g = new Goal(GoalKind.SERVE_LORD, lord.id,
                        lord.displayName + "을(를) 주군으로 섬긴다");
                g.priority = 55;
                candidates.add(g);
            }
        }

        // 배신 — 충성 매우 낮음 + 세력 있음
        if (loyalty <= -50 && !npc.faction.isEmpty()) {
            Goal g = new Goal(GoalKind.BETRAY, npc.faction,
                    npc.faction + "을(를) 배신한다");
            g.priority = 65;
            candidates.add(g);
        }

        // 재물
        int greed = p.get(Personality.Trait.GREED);
        if (greed >= 70) {
            Goal g = new Goal(GoalKind.GAIN_WEALTH, "",
                    "큰 재물을 모은다");
            g.priority = 50;
            candidates.add(g);
        } else if (greed >= 40 && "MERCHANT".equals(npc.job)) {
            Goal g = new Goal(GoalKind.START_BUSINESS, "",
                    "내 상점을 차린다");
            g.priority = 55;
            candidates.add(g);
        }

        // 사랑 (LOVE 욕구 + 미혼)
        if (n.get(Needs.Kind.LOVE) < 30 && npc.spouseNpcId.isEmpty()) {
            Goal g = new Goal(GoalKind.FIND_LOVE, "",
                    "사랑할 짝을 찾는다");
            g.priority = 60;
            candidates.add(g);
        }

        // 가족 보호 (가족 있음)
        if (!npc.soul.family.isEmpty()) {
            Goal g = new Goal(GoalKind.PROTECT_FAMILY, "",
                    "가족을 지킨다");
            g.priority = 75;
            candidates.add(g);
        }

        // 탐험
        int curiosity = p.get(Personality.Trait.CURIOSITY);
        if (curiosity >= 70) {
            Goal g = new Goal(GoalKind.EXPLORE, "",
                    "세상을 본다");
            g.priority = 40;
            candidates.add(g);
        }
        if (curiosity >= 60 && ("SAGE".equals(npc.job) || "SCHOLAR".equals(npc.job))) {
            Goal g = new Goal(GoalKind.ACCUMULATE_KNOWLEDGE, "",
                    "지식을 쌓는다");
            g.priority = 60;
            candidates.add(g);
        }

        // 라이벌 격파
        if (p.get(Personality.Trait.PRIDE) >= 70 && !npc.soul.rivals.isEmpty()) {
            String rival = npc.soul.rivals.get(0);
            Goal g = new Goal(GoalKind.DEFEAT_RIVAL, rival,
                    "라이벌 " + shortenId(rival) + "을(를) 꺾는다");
            g.priority = 65;
            candidates.add(g);
        }
        // 소문 기반 — 직접 안 만났어도 평판 나쁜 자를 응징 (정의감 = EMPATHY 또는 자존심)
        String infamous = npc.soul.reputation.worstReputation(70);
        if (infamous != null && (p.get(Personality.Trait.EMPATHY) >= 50
                || p.get(Personality.Trait.AGGRESSION) >= 60)) {
            Goal g = new Goal(GoalKind.DEFEAT_RIVAL, infamous,
                    "악명 높은 " + shortenId(infamous) + "을(를) 응징한다");
            g.priority = 60;
            candidates.add(g);
        }
        // 소문 기반 — 평판 좋은 자를 주군으로 (LOYALTY 높을 때, 소문만으로 흠모)
        String renowned = npc.soul.reputation.bestReputation(70);
        if (renowned != null && p.get(Personality.Trait.LOYALTY) >= 60
                && npc.spouseNpcId.isEmpty()) {
            Goal g = new Goal(GoalKind.SERVE_LORD, renowned,
                    "명성 높은 " + shortenId(renowned) + "을(를) 섬긴다");
            g.priority = 50;
            candidates.add(g);
        }

        // 기예 수련 — 무인·도사·수련자
        if (isMartialJob(npc.job) || isMagicalJob(npc.job)) {
            Goal g = new Goal(GoalKind.MASTER_ART, "",
                    "기예를 극한까지 수련한다");
            g.priority = 50;
            candidates.add(g);
        }

        // 종교 창시 — 사제 + 공감
        if (("PRIEST".equals(npc.job) || "MONK".equals(npc.job))
                && p.get(Personality.Trait.EMPATHY) >= 50) {
            Goal g = new Goal(GoalKind.FOUND_RELIGION, "",
                    "새 종교를 창시한다");
            g.priority = 70;
            candidates.add(g);
        }

        // 적대 세력 멸살
        if (!npc.soul.nemeses.isEmpty() && p.get(Personality.Trait.AGGRESSION) >= 60) {
            Goal g = new Goal(GoalKind.DESTROY_RIVAL_FACTION, "",
                    "원수의 세력을 멸한다");
            g.priority = 75;
            candidates.add(g);
        }

        // 은둔 — 사교성 매우 낮음
        if (p.get(Personality.Trait.SOCIABILITY) <= -50) {
            Goal g = new Goal(GoalKind.HIDE, "", "세상을 등지고 숨어 산다");
            g.priority = 45;
            candidates.add(g);
        }

        if (candidates.isEmpty()) return null;
        // 우선순위 가장 높은 것 (같으면 무작위)
        candidates.sort((a, b) -> Integer.compare(b.priority, a.priority));
        int topPri = candidates.get(0).priority;
        List<Goal> top = new ArrayList<>();
        for (Goal g : candidates) {
            if (g.priority == topPri) top.add(g);
            else break;
        }
        return top.get(Rand.range(0, top.size() - 1));
    }

    private boolean hasClanRank(RebornNpc npc, String rank) {
        // TODO: RebornClan 연동 — 현재는 job 기반 추정
        return "KING".equals(npc.job) || "EMPEROR".equals(npc.job)
                || "DEMON_LORD".equals(npc.job) || "CULT_MASTER".equals(npc.job);
    }

    private RebornNpc findStrongestInFaction(RebornNpc self) {
        RebornNpc best = null;
        double bestT = 0;
        for (RebornNpc other : plugin.registry().all()) {
            if (other == self || other.dead) continue;
            if (!self.faction.equals(other.faction)) continue;
            double t = other.effectiveTotal();
            if (t > bestT) { bestT = t; best = other; }
        }
        return best;
    }

    private boolean isMartialJob(String job) {
        return "WARRIOR".equals(job) || "GUARD".equals(job) || "ASSASSIN".equals(job)
                || "SECT_LEADER".equals(job) || "ABBOT".equals(job);
    }

    private boolean isMagicalJob(String job) {
        return "MAGE".equals(job) || "ARCHANGEL".equals(job) || "PRIEST".equals(job)
                || "SPIRIT_KING".equals(job) || "ALCHEMIST".equals(job)
                || "PALACE_MASTER".equals(job);
    }

    private String shortenId(String id) {
        if (id.length() <= 12) return id;
        return id.substring(0, 8) + "...";
    }
}
