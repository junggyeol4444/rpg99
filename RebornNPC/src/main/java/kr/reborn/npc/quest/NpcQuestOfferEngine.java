package kr.reborn.npc.quest;

import kr.reborn.core.util.Rand;
import kr.reborn.npc.RebornNPC;
import kr.reborn.npc.entity.RebornNpc;
import kr.reborn.npc.soul.Needs;
import org.bukkit.Bukkit;

/**
 * NPC 자율 의뢰 시스템 (기획서 7장 ①).
 *
 * NPC가 자신의 욕구·기억·감정에 따라 스스로 퀘스트를 생성하여
 * 플레이어가 만났을 때 의뢰한다.
 *
 * 의뢰 생성 조건 (욕구 기반):
 *   FOOD < 30           → "사냥감 가져와줘" (KILL N 가축류)
 *   COMPANIONSHIP < 30  → "사람을 데려와줘" (TALK_TO N)
 *   STATUS < 30         → "내 명성을 알려줘" (EXPLORE 가까운 마을)
 *   SAFETY < 30         → "위협 제거해줘" (KILL N 적대 몬스터)
 *
 * 의뢰 보상:
 *   - 호감도 +10~30 (욕구 충족 강도에 비례)
 *   - 스탯 소량 +
 *   - 만료: 24시간 (자동 폐기 + 호감도 -5)
 *
 * 의뢰는 RebornQuest.QuestRegistry에 동적 등록되며, 일반 퀘스트와 동일하게 추적.
 */
public final class NpcQuestOfferEngine {

    private static final long OFFER_TTL_MS = 24L * 3600_000L;
    /** 한 NPC가 의뢰를 새로 만들기 전 최소 간격 — 스팸 방지. */
    private static final long OFFER_INTERVAL_MS = 3600_000L;

    private final RebornNPC plugin;

    public NpcQuestOfferEngine(RebornNPC plugin) {
        this.plugin = plugin;
    }

    /** NpcRegistry.tickAll에서 NPC 1개당 주기적으로 호출. */
    public void considerOffer(RebornNpc npc) {
        if (npc.dead || npc.soul == null) return;
        long now = System.currentTimeMillis();
        // 기존 의뢰가 있으면 만료 검사
        if (npc.pendingQuestOffer != null) {
            if (now - npc.pendingQuestOfferAt > OFFER_TTL_MS) {
                expirePending(npc);
            }
            return;  // 의뢰가 있는 동안 새로 안 만듦
        }
        // 욕구가 낮을 때만 의뢰 — 욕구 충족된 NPC는 의뢰 없음 (기획 합치)
        Needs n = npc.soul.needs;
        String type;
        String target;
        int amount;
        String description;
        if (n.get(Needs.Kind.FOOD) < 30) {
            type = "KILL"; target = "COW"; amount = 5;
            description = "배가 고프니 가축을 잡아오세요.";
        } else if (n.get(Needs.Kind.SAFETY) < 30) {
            type = "KILL"; target = "ZOMBIE"; amount = 10;
            description = "주변이 무서워 몬스터를 처치해주세요.";
        } else if (n.get(Needs.Kind.STATUS) < 30) {
            type = "EXPLORE"; target = ""; amount = 3;
            description = "내 이름을 멀리 알려주세요. 3개 마을을 방문.";
        } else if (n.get(Needs.Kind.COMPANIONSHIP) < 30) {
            type = "EXPLORE"; target = ""; amount = 1;
            description = "외로워서 누군가와 함께 있고 싶다.";
        } else {
            return;  // 욕구 모두 충족 — 의뢰 없음
        }
        // 호출 빈도 제한
        Long last = (Long) npc.aiData.get("lastQuestOffer");
        if (last != null && now - last < OFFER_INTERVAL_MS) return;
        if (!Rand.chance(0.1)) return;  // 10% 확률로 생성 (스팸 방지)

        // 동적 퀘스트 등록 (RebornQuest 리플렉션)
        try {
            var qp = Bukkit.getPluginManager().getPlugin("RebornQuest");
            if (qp == null) return;
            Object registry = qp.getClass().getMethod("registry").invoke(qp);
            String qid = "npc_" + npc.id + "_" + (now / 1000);
            // Quest(id, name, type, target, amount, world, linked, rewards)
            Class<?> questCls = Class.forName("kr.reborn.quest.engine.Quest");
            Object quest = questCls.getConstructor(String.class, String.class, String.class,
                            String.class, int.class, String.class, String.class, java.util.Map.class)
                    .newInstance(qid,
                            "[" + npc.displayName + "] " + description,
                            type, target, amount,
                            npc.world == null ? "" : npc.world.name(),
                            null,
                            java.util.Map.of("favor", 20, "stats",
                                    java.util.Map.of("CHARISMA", 1)));
            registry.getClass().getMethod("register", questCls).invoke(registry, quest);
            npc.pendingQuestOffer = qid;
            npc.pendingQuestOfferAt = now;
            npc.aiData.put("lastQuestOffer", now);
        } catch (Throwable ignored) {}
    }

    private void expirePending(RebornNpc npc) {
        npc.pendingQuestOffer = null;
        npc.pendingQuestOfferAt = 0;
    }

    /** NpcInteractListener에서 호출 — 만난 플레이어에게 의뢰가 있으면 안내. */
    public String pendingOfferOf(RebornNpc npc) {
        return npc.pendingQuestOffer;
    }
}
