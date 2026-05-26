package kr.reborn.npc.social;

import kr.reborn.core.util.Rand;
import kr.reborn.npc.RebornNPC;
import kr.reborn.npc.ai.utility.ResponseCurve;
import kr.reborn.npc.entity.RebornNpc;
import kr.reborn.npc.soul.Personality;
import org.bukkit.Bukkit;

import java.util.List;

/**
 * 소문 생성·전파 중앙 관리자. NpcRegistry에 1개 인스턴스.
 *
 * 전파 모델:
 *   - 매 사이클 propagate() 호출
 *   - 같은 월드 + 8블록 이내 NPC 쌍이 만나면 소문 교환
 *   - SOCIABILITY 높은 NPC가 더 잘 퍼뜨림
 *   - 같은 세력 출처면 believability 가중 (편향)
 *   - 전달마다 hopCount+1, believability×0.85 (변질)
 *
 * 사건 발생 시 createRumor()로 1차 소문 생성 (목격자 = originator).
 */
public final class GossipManager {

    private final RebornNPC plugin;
    private static final double GOSSIP_RANGE = 8.0;

    public GossipManager(RebornNPC plugin) { this.plugin = plugin; }

    /** 사건 발생 — 목격자에게 1차 소문 부여. */
    public void createRumor(RebornNpc witness, String subject, String object,
                            RumorContent content, int severity) {
        if (witness == null || witness.soul == null) return;
        Rumor rumor = Rumor.fresh(witness.id, subject, object, content, severity);
        addToNpc(witness, rumor);
        // 큰 사건은 같은 청크 NPC 전부 즉시 목격
        if (content.isMajor() && witness.location != null) {
            for (RebornNpc near : plugin.registry().all()) {
                if (near == witness || near.dead || near.soul == null) continue;
                if (near.location == null || near.location.getWorld() != witness.location.getWorld()) continue;
                if (near.location.distance(witness.location) <= 16) {
                    addToNpc(near, Rumor.fresh(near.id, subject, object, content, severity));
                }
            }
        }
    }

    /** 매 사이클 — 근처 NPC끼리 소문 교환. (cost 절약: 일부만 샘플링) */
    public void propagate() {
        var all = new java.util.ArrayList<>(plugin.registry().all());
        if (all.size() < 2) return;
        // 성능: 매 사이클 최대 30쌍만 처리
        int attempts = Math.min(30, all.size());
        for (int i = 0; i < attempts; i++) {
            RebornNpc a = all.get(Rand.range(0, all.size() - 1));
            if (a.dead || a.soul == null || a.location == null) continue;
            if (a.soul.rumorsHeard.isEmpty()) continue;
            RebornNpc b = findGossipPartner(a);
            if (b == null) continue;
            exchange(a, b);
        }
    }

    private RebornNpc findGossipPartner(RebornNpc a) {
        RebornNpc best = null;
        double bestD = GOSSIP_RANGE;
        for (RebornNpc b : plugin.registry().all()) {
            if (b == a || b.dead || b.soul == null || b.location == null) continue;
            if (b.location.getWorld() != a.location.getWorld()) continue;
            // 원수에게는 소문 안 전함
            if (a.soul.relationToward(b.id) < -50) continue;
            double d = b.location.distance(a.location);
            if (d < bestD) { bestD = d; best = b; }
        }
        return best;
    }

    private void exchange(RebornNpc a, RebornNpc b) {
        // a → b 로 소문 전달 (SOCIABILITY로 확률)
        double talkChance = ResponseCurve.linear(
                a.soul.personality.get(Personality.Trait.SOCIABILITY), -50, 100);
        if (!Rand.chance(0.3 + 0.5 * talkChance)) return;

        // a의 소문 중 1~2개 무작위로 전달
        List<Rumor> pool = a.soul.rumorsHeard;
        int count = Math.min(pool.size(), Rand.range(1, 2));
        for (int i = 0; i < count; i++) {
            Rumor original = pool.get(Rand.range(0, pool.size() - 1));
            if (!original.isViable()) continue;
            // b가 이미 같은 사건 알면 스킵
            boolean known = b.soul.rumorsHeard.stream()
                    .anyMatch(r -> r.eventKey().equals(original.eventKey()));
            if (known) continue;
            Rumor retold = original.retold();
            // 같은 세력 출처면 신뢰도 가중 (편향)
            RebornNpc originatorNpc = plugin.registry().get(original.originator);
            if (originatorNpc != null && !a.faction.isEmpty()
                    && a.faction.equals(originatorNpc.faction)) {
                // believability를 약간 회복 (편향: 우리 편 말은 믿음)
                retold = new Rumor(retold.originator, retold.subject, retold.object,
                        retold.content, retold.severity,
                        Math.min(1.0, retold.believability * 1.2),
                        retold.hopCount, retold.createdAt);
            }
            addToNpc(b, retold);
        }
    }

    private void addToNpc(RebornNpc npc, Rumor rumor) {
        npc.soul.rumorsHeard.add(rumor);
        npc.soul.reputation.absorb(rumor);
        // 50건 초과 시 가장 오래된 것 제거
        if (npc.soul.rumorsHeard.size() > 50) {
            npc.soul.rumorsHeard.remove(0);
        }
        // 평판이 충격적이면 분류 재평가
        if (rumor.content.isMajor()) {
            npc.soul.reclassify(rumor.subject);
        }
    }
}
