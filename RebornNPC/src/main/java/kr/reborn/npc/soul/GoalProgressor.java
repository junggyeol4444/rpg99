package kr.reborn.npc.soul;

import kr.reborn.npc.RebornNPC;
import kr.reborn.npc.entity.RebornNpc;
import org.bukkit.Bukkit;

/**
 * 활성 목표의 진행률을 갱신.
 *
 * 두 가지 방식:
 *   1) tick() — 매 사이클 자연 진행 (천천히, kind에 따라 다른 속도)
 *   2) onEvent() — 특정 이벤트에 큰 점프 (외부에서 호출)
 *
 * 완료된 목표는 NPC에게 보상 (감정·욕구·기억).
 */
public final class GoalProgressor {

    private final RebornNPC plugin;

    public GoalProgressor(RebornNPC plugin) { this.plugin = plugin; }

    /** 매 사이클 호출. 모든 활성 목표 자연 진행. */
    public void tick(RebornNpc npc) {
        if (npc.soul == null || npc.dead || npc.goals.isEmpty()) return;
        for (Goal g : npc.goals) {
            if (!g.isActive()) continue;
            double delta = naturalProgressRate(g, npc);
            g.advance(delta);
            if (g.isFulfilled()) onCompleted(npc, g);
            // 불가능 판단 — 너무 오래 진행 안 됨
            else if (System.currentTimeMillis() - g.createdAt > 86_400_000L * 30
                    && g.progress < 10) {
                g.abandoned = true;
                if (npc.soul != null) {
                    npc.soul.needs.add(Needs.Kind.ACHIEVEMENT, -10);
                    npc.emotion.add(kr.reborn.npc.emotion.Emotion.Kind.SADNESS, 20);
                }
            }
        }
    }

    /** 자연 진행 속도 (퍼센트/사이클). 매우 느림 — 큰 점프는 이벤트로. */
    private double naturalProgressRate(Goal g, RebornNpc npc) {
        switch (g.kind) {
            case MASTER_ART:           return 0.05; // 수련은 매우 느림
            case ACCUMULATE_KNOWLEDGE: return 0.08;
            case GAIN_WEALTH:          return 0.02;
            case GAIN_POWER:           return 0.03;
            case ASCEND:               return 0.01; // 가장 느림
            case FOUND_TOWN:           return 0.04;
            case FOUND_RELIGION:       return 0.03;
            case EXPLORE:              return 0.10;
            case HIDE:                 return 0.50; // 그냥 숨어있으면 빨리 달성
            case SERVE_LORD:           return 0.10; // 옆에 있기만 해도 진행
            case PROTECT_FAMILY:       return 0.05; // 무사하면 진행
            case FIND_LOVE:            return 0.00; // 결혼 이벤트로만
            case AVENGE:               return 0.00; // 대상 살해 이벤트로만
            case DEFEAT_RIVAL:         return 0.00; // 대상 격파 이벤트로만
            case BETRAY:               return 0.02;
            case DESTROY_RIVAL_FACTION:return 0.01;
            case START_BUSINESS:       return 0.05;
            default:                    return 0.01;
        }
    }

    /** 외부 이벤트에 따른 진행 점프. */
    public void onEvent(RebornNpc npc, Event event) {
        if (npc.soul == null || npc.goals.isEmpty()) return;
        for (Goal g : npc.goals) {
            if (!g.isActive()) continue;
            double delta = eventDelta(g, event);
            if (delta > 0) {
                g.advance(delta);
                if (g.isFulfilled()) onCompleted(npc, g);
            }
        }
    }

    private double eventDelta(Goal g, Event event) {
        switch (event.kind) {
            case MARRIED:
                if (g.kind == GoalKind.FIND_LOVE) return 100;
                if (g.kind == GoalKind.GAIN_POWER && event.payload.startsWith("ROYAL")) return 30;
                break;
            case KILLED_TARGET:
                if (g.kind == GoalKind.AVENGE && g.target.equals(event.payload)) return 100;
                if (g.kind == GoalKind.DEFEAT_RIVAL && g.target.equals(event.payload)) return 100;
                if (g.kind == GoalKind.DESTROY_RIVAL_FACTION) return 15;
                break;
            case TIER_UP:
                if (g.kind == GoalKind.MASTER_ART) return 25;
                if (g.kind == GoalKind.ASCEND) return 10;
                break;
            case CLAN_FOUNDED:
                if (g.kind == GoalKind.FOUND_TOWN) return 60;
                if (g.kind == GoalKind.GAIN_POWER) return 25;
                break;
            case KINGDOM_FOUNDED:
                if (g.kind == GoalKind.GAIN_POWER) return 70;
                if (g.kind == GoalKind.ASCEND) return 30;
                break;
            case TERRITORY_CLAIMED:
                if (g.kind == GoalKind.GAIN_POWER) return 15;
                if (g.kind == GoalKind.FOUND_TOWN) return 30;
                break;
            case WEALTH_GAINED:
                if (g.kind == GoalKind.GAIN_WEALTH) {
                    double amount = Double.parseDouble(event.payload);
                    return amount / 10000.0;  // 10000골드당 1%
                }
                break;
            case SHOP_OPENED:
                if (g.kind == GoalKind.START_BUSINESS) return 100;
                if (g.kind == GoalKind.GAIN_WEALTH) return 20;
                break;
            case RELIGION_FOUNDED:
                if (g.kind == GoalKind.FOUND_RELIGION) return 100;
                break;
            case BECAME_GOD:
                if (g.kind == GoalKind.ASCEND) return 100;
                break;
            case FAMILY_LOST:
                if (g.kind == GoalKind.PROTECT_FAMILY) {
                    g.abandoned = true;  // 실패
                    return 0;
                }
                break;
            case LORD_DIED:
                if (g.kind == GoalKind.SERVE_LORD && g.target.equals(event.payload)) {
                    g.abandoned = true;  // 주군 죽음 — 목표 무효
                    return 0;
                }
                break;
            case WORLD_EXPLORED:
                if (g.kind == GoalKind.EXPLORE) return 20;
                break;
            case KNOWLEDGE_GAINED:
                if (g.kind == GoalKind.ACCUMULATE_KNOWLEDGE) return 5;
                break;
            case FACTION_BETRAYED:
                if (g.kind == GoalKind.BETRAY) return 100;
                break;
        }
        return 0;
    }

    private void onCompleted(RebornNpc npc, Goal g) {
        if (npc.soul == null) return;
        // 완료 보상 — 큰 만족 + 행복
        npc.soul.needs.add(Needs.Kind.ACHIEVEMENT, +40);
        npc.soul.needs.add(Needs.Kind.STATUS, +20);
        npc.emotion.add(kr.reborn.npc.emotion.Emotion.Kind.HAPPINESS, 40);
        // 세계에 실제 영향 — 마을·종교·왕국·상점을 진짜로 만든다
        plugin.registry().worldImpact().apply(npc, g);
        // 복수 완료는 특히 큰 카타르시스
        if (g.kind == GoalKind.AVENGE) {
            npc.emotion.add(kr.reborn.npc.emotion.Emotion.Kind.ANGER, -50);
            npc.emotion.add(kr.reborn.npc.emotion.Emotion.Kind.SADNESS, -30);
        }
        // 공개 메시지 — 큰 목표만
        if (g.priority >= 70) {
            Bukkit.broadcastMessage("§6§l[NPC 성취] §f" + npc.displayName
                    + " — " + g.description + " §a✓");
        }
    }

    /** 외부에서 호출하는 이벤트 타입. */
    public enum EventKind {
        MARRIED, KILLED_TARGET, TIER_UP, CLAN_FOUNDED, KINGDOM_FOUNDED,
        TERRITORY_CLAIMED, WEALTH_GAINED, SHOP_OPENED, RELIGION_FOUNDED,
        BECAME_GOD, FAMILY_LOST, LORD_DIED, WORLD_EXPLORED, KNOWLEDGE_GAINED,
        FACTION_BETRAYED
    }

    public static final class Event {
        public final EventKind kind;
        public final String payload;
        public Event(EventKind kind, String payload) { this.kind = kind; this.payload = payload == null ? "" : payload; }
    }
}
