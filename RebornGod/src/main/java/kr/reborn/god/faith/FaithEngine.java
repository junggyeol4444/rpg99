package kr.reborn.god.faith;

import kr.reborn.god.RebornGod;
import kr.reborn.god.data.God;
import kr.reborn.god.data.Religion;

/**
 * 신앙 누적 → 신성 변환 엔진.
 *
 * 매 주기:
 *  1) 각 교단이 신도 수에 비례해 신앙(faith)을 얻는다 (NPC 자동 기도 가정).
 *  2) 교단의 신앙은 일정 비율로 신성(divinity)으로 변환되어 섬기는 신에게 흐른다.
 *  3) 시간 경과로 신앙이 천천히 감쇠 (망각).
 *  4) 적대 교단의 신도가 신앙을 깎는 효과도 반영.
 *
 * 이게 진짜 "신앙으로 신이 강해진다"의 핵심 루프.
 */
public final class FaithEngine {

    /** 교단 신앙 → 신성 변환 비율 (신앙 100 → 신성 1). */
    private static final double FAITH_TO_DIVINITY = 0.01;
    /** 시간당 신앙 자연 감쇠율. */
    private static final double FAITH_DECAY_PER_CYCLE = 0.001;
    /** 적대 교단이 깎는 추가 신앙 비율. */
    private static final double ANTI_DRAIN = 0.005;

    private final RebornGod plugin;

    public FaithEngine(RebornGod plugin) { this.plugin = plugin; }

    /** 한 주기 — 모든 교단에 적용. RebornGod 스케줄러가 주기적으로 호출. */
    public void tick() {
        for (Religion r : plugin.religions().all()) {
            // 1) 신앙 축적
            r.faith += r.faithGainPerCycle();
            // 2) 신앙 자연 감쇠
            r.faith *= (1.0 - FAITH_DECAY_PER_CYCLE);
            // 3) 적대 교단이 있으면 추가 감쇠
            if (!r.antiReligion.isEmpty()) {
                Religion anti = plugin.religions().get(r.antiReligion);
                if (anti != null) {
                    double drain = anti.totalFollowers() * ANTI_DRAIN;
                    r.faith = Math.max(0, r.faith - drain);
                }
            }
            // 4) 신앙 → 신성 변환 (섬기는 신에게)
            God g = resolveGod(r.godIdentifier);
            if (g != null && !g.sealed) {
                double convert = r.faith * FAITH_TO_DIVINITY;
                if (convert > 0.1) {
                    r.faith -= convert;
                    g.divinity += convert;
                    g.influence = computeInfluence(g);
                }
            }
        }
    }

    /** 플레이어 기도 — 신앙 즉시 가산 + 작은 축복. */
    public boolean pray(org.bukkit.entity.Player p, String religionId) {
        Religion r = plugin.religions().get(religionId);
        if (r == null) return false;
        // 신도 등록
        r.followers.add(p.getUniqueId());
        r.faith += 5;  // 한 번 기도 = +5 신앙
        // 신에게 즉시 1 신성 흐름
        God g = resolveGod(r.godIdentifier);
        if (g != null && !g.sealed) g.divinity += 0.5;
        // 작은 축복 — 행운+1 임시 (RebornCurse 연동 필요 시 추후)
        p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.LUCK, 1200, 0, false, false));
        return true;
    }

    /** 신 식별자(npc:xxx 또는 player:UUID) → 실제 신 인스턴스. */
    private God resolveGod(String identifier) {
        if (identifier == null || identifier.isEmpty()) return null;
        if (identifier.startsWith("npc:")) {
            String npcId = identifier.substring(4);
            for (God g : plugin.gods().npcAll()) {
                if (npcId.equals(g.npcId)) return g;
            }
            return null;
        }
        if (identifier.startsWith("player:")) {
            try {
                java.util.UUID u = java.util.UUID.fromString(identifier.substring(7));
                return plugin.gods().of(u);
            } catch (IllegalArgumentException e) { return null; }
        }
        // identifier가 그냥 npc id인 경우
        for (God g : plugin.gods().npcAll()) {
            if (identifier.equals(g.npcId)) return g;
        }
        return null;
    }

    private double computeInfluence(God g) {
        return g.followers.size() * 1.0 + g.allies.size() * 5.0 + Math.log10(Math.max(1, g.divinity));
    }
}
