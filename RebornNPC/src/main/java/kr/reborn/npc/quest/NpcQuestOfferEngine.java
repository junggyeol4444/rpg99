package kr.reborn.npc.quest;

import kr.reborn.core.event.RebornQuestCompleteEvent;
import kr.reborn.core.util.Rand;
import kr.reborn.npc.RebornNPC;
import kr.reborn.npc.entity.RebornNpc;
import kr.reborn.npc.soul.Memory;
import kr.reborn.npc.soul.Needs;
import kr.reborn.npc.soul.Personality;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

/**
 * NPC 자율 의뢰 시스템 (기획서 7장 ①).
 *
 * NPC가 욕구·기억·감정·직업·성격에 따라 스스로 퀘스트를 생성한다.
 *
 * 의뢰 생성 우선순위 (높을수록 우선):
 *   1. 강렬한 기억 — KILLED_MY_FAMILY → 복수 (AGGRESSION 가중)
 *   2. 욕구 임계치 — 9종 욕구 별 의뢰
 *   3. 직업 — 대장장이 재료, 상인 호위, 사제 정화 등
 *   4. 성격 — GREED 높음 → 보물 회수, EMPATHY 높음 → 약자 구조
 *
 * 보상은 욕구/기억/직업 강도에 비례하여 동적 결정.
 * 만료(24h)되면 호감도 감소 — NPC가 "그 의뢰 무시했군" 기억.
 */
public final class NpcQuestOfferEngine implements Listener {

    private static final long OFFER_TTL_MS = 24L * 3600_000L;
    private static final long OFFER_INTERVAL_MS = 3600_000L;

    public static final class OfferTemplate {
        public final String type;      // KILL/GATHER/EXPLORE/TALK/CRAFT/SURVIVE
        public final String target;
        public final int amount;
        public final String desc;
        public final int favor;
        public final String rewardStat;
        public final int rewardAmount;
        public final String tag;       // 의뢰 분류 (need/job/memory/personality)
        public OfferTemplate(String type, String target, int amount, String desc,
                             int favor, String rewardStat, int rewardAmount, String tag) {
            this.type = type; this.target = target; this.amount = amount; this.desc = desc;
            this.favor = favor; this.rewardStat = rewardStat; this.rewardAmount = rewardAmount;
            this.tag = tag;
        }
    }

    private final RebornNPC plugin;

    public NpcQuestOfferEngine(RebornNPC plugin) {
        this.plugin = plugin;
    }

    public void considerOffer(RebornNpc npc) {
        if (npc.dead || npc.soul == null) return;
        long now = System.currentTimeMillis();
        if (npc.pendingQuestOffer != null) {
            if (now - npc.pendingQuestOfferAt > OFFER_TTL_MS) {
                expirePending(npc);
            }
            return;
        }
        Long last = (Long) npc.aiData.get("lastQuestOffer");
        if (last != null && now - last < OFFER_INTERVAL_MS) return;

        List<OfferTemplate> candidates = generateCandidates(npc);
        if (candidates.isEmpty()) return;
        if (!Rand.chance(0.12)) return;

        // 우선순위·중요도에 따라 무작위 선택 (성격·필요로 약간 가중)
        OfferTemplate pick = candidates.get(Rand.range(0, candidates.size() - 1));
        registerOffer(npc, pick, now);
    }

    /** 20+종 의뢰 후보 생성 — 욕구·기억·직업·성격 조합. */
    private List<OfferTemplate> generateCandidates(RebornNpc npc) {
        List<OfferTemplate> out = new ArrayList<>();
        Needs n = npc.soul.needs;
        Personality p = npc.soul.personality;

        // ─── 욕구 9종 ───
        if (n.get(Needs.Kind.FOOD) < 30) {
            out.add(new OfferTemplate("KILL", "COW", 5,
                    "배가 고프다. 가축 5마리를 잡아주게.",
                    15, "ENDURANCE", 1, "need-food"));
            out.add(new OfferTemplate("GATHER", "BREAD", 16,
                    "마을이 굶주린다. 빵 16덩이를 가져와다오.",
                    18, "CHARISMA", 1, "need-food"));
        }
        if (n.get(Needs.Kind.REST) < 30) {
            out.add(new OfferTemplate("KILL", "PHANTOM", 3,
                    "악몽이 잠을 방해한다. 팬텀 3마리를 처치해줘.",
                    22, "MENTAL", 2, "need-rest"));
        }
        if (n.get(Needs.Kind.SAFETY) < 30) {
            out.add(new OfferTemplate("KILL", "ZOMBIE", 10,
                    "마을 주변 좀비가 무섭다. 10마리만.",
                    20, "STRENGTH", 1, "need-safety"));
            out.add(new OfferTemplate("KILL", "SKELETON", 8,
                    "화살이 자주 날아온다. 스켈레톤 8을 정리해줘.",
                    20, "AGILITY", 1, "need-safety"));
        }
        if (n.get(Needs.Kind.COMPANIONSHIP) < 30) {
            out.add(new OfferTemplate("EXPLORE", "", 2,
                    "외로우니 함께 마을 2곳을 둘러봐 주게.",
                    18, "CHARISMA", 1, "need-social"));
        }
        if (n.get(Needs.Kind.LOVE) < 25 && npc.spouseNpcId.isEmpty()) {
            out.add(new OfferTemplate("GATHER", "POPPY", 5,
                    "내게 청혼할 용기를 주오. 붉은 꽃 5송이를…",
                    25, "CHARM", 2, "need-love"));
        }
        if (n.get(Needs.Kind.STATUS) < 30) {
            out.add(new OfferTemplate("EXPLORE", "", 3,
                    "내 이름을 멀리 알려라. 3개 마을을 방문하라.",
                    20, "CHARISMA", 2, "need-status"));
            out.add(new OfferTemplate("KILL", "PILLAGER", 5,
                    "약탈자 5명의 머리를 베어와 내 이름을 알려라.",
                    25, "STRENGTH", 2, "need-status"));
        }
        if (n.get(Needs.Kind.ACHIEVEMENT) < 30) {
            out.add(new OfferTemplate("KILL", "WITCH", 1,
                    "내가 평생 못 잡은 마녀를 자네가 잡아주게.",
                    30, "INTELLIGENCE", 3, "need-achievement"));
        }
        if (n.get(Needs.Kind.MASTERY) < 30) {
            out.add(new OfferTemplate("GATHER", "BOOK", 3,
                    "고대 서적 3권이 필요하다. 가르침을 위해.",
                    25, "INTELLIGENCE", 2, "need-mastery"));
        }
        if (n.get(Needs.Kind.AUTONOMY) < 25) {
            out.add(new OfferTemplate("EXPLORE", "", 1,
                    "이 마을이 답답하다. 새 정착지를 찾아 다오.",
                    22, "LUCK", 2, "need-autonomy"));
        }

        // ─── 강렬한 기억 (복수) ───
        Memory mem = npc.soul.memory;
        java.util.List<Memory.Entry> all = mem.all();
        int from = Math.max(0, all.size() - 10);
        for (Memory.Entry me : all.subList(from, all.size())) {
            if (me.currentIntensity() < 30) continue;
            if (me.kind == Memory.Kind.KILLED_MY_FAMILY || me.kind == Memory.Kind.KILLED_MY_FRIEND) {
                out.add(new OfferTemplate("KILL", "ZOMBIE", 20,
                        "가족/벗을 잃었다. 살해자에 합당한 응징을. 좀비 20마리를 추적·소탕.",
                        50, "STRENGTH", 5, "memory-revenge"));
                break;
            } else if (me.kind == Memory.Kind.STOLE_FROM_ME) {
                out.add(new OfferTemplate("GATHER", "EMERALD", 5,
                        "도둑이 내 보물을 가져갔다. 에메랄드 5개로 갚아라.",
                        35, "LUCK", 2, "memory-theft"));
                break;
            } else if (me.kind == Memory.Kind.SAVED_MY_LIFE && me.currentIntensity() > 50) {
                out.add(new OfferTemplate("EXPLORE", "", 1,
                        "은인 자네의 명성을 알리고 싶다. 1개 마을을 방문해줘.",
                        30, "CHARISMA", 3, "memory-debt"));
                break;
            }
        }

        // ─── 직업별 의뢰 ───
        String job = npc.job == null ? "" : npc.job.toUpperCase();
        switch (job) {
            case "BLACKSMITH", "DWARF_BLACKSMITH" -> out.add(new OfferTemplate(
                    "GATHER", "IRON_INGOT", 32,
                    "철괴 32개가 필요하다 — 단조 작업에 부족하다.",
                    25, "STRENGTH", 2, "job-smith"));
            case "ALCHEMIST", "PILLMASTER" -> out.add(new OfferTemplate(
                    "GATHER", "GLISTERING_MELON_SLICE", 6,
                    "단약 제조에 빛나는 멜론 조각 6개가 필요하다.",
                    28, "INTELLIGENCE", 2, "job-alch"));
            case "FARMER" -> out.add(new OfferTemplate(
                    "KILL", "RABBIT", 8,
                    "토끼들이 작물을 망친다. 8마리 처리.",
                    15, "ENDURANCE", 1, "job-farm"));
            case "HUNTER" -> out.add(new OfferTemplate(
                    "KILL", "WOLF", 6,
                    "늑대 가죽이 필요하다. 6마리.",
                    22, "AGILITY", 2, "job-hunter"));
            case "MERCHANT", "GOLD_MERCHANT" -> out.add(new OfferTemplate(
                    "GATHER", "GOLD_INGOT", 8,
                    "내 가게의 재고가 떨어졌다. 금괴 8개를.",
                    30, "CHARISMA", 2, "job-merchant"));
            case "PRIEST", "ARCHBISHOP" -> out.add(new OfferTemplate(
                    "KILL", "SKELETON", 13,
                    "언데드 13체를 정화해다오. 신의 이름으로.",
                    35, "MENTAL", 3, "job-priest"));
            case "MAGE", "ARCHMAGE" -> out.add(new OfferTemplate(
                    "GATHER", "AMETHYST_SHARD", 12,
                    "수정의 파편 12조각으로 마법진을 그리겠다.",
                    32, "INTELLIGENCE", 3, "job-mage"));
            case "ELDER", "WISE_ONE" -> out.add(new OfferTemplate(
                    "EXPLORE", "", 5,
                    "젊은이여, 5개 마을을 둘러보고 경험을 쌓아 오게.",
                    40, "MENTAL", 5, "job-elder"));
            case "KING", "EMPEROR" -> out.add(new OfferTemplate(
                    "KILL", "PILLAGER", 3,
                    "왕국에 도전한 약탈단을 처치하라.",
                    60, "CHARISMA", 5, "job-king"));
            case "KNIGHT", "PALADIN" -> out.add(new OfferTemplate(
                    "KILL", "ENDERMAN", 3,
                    "엔더맨 3체를 베어와 정의를 입증하라.",
                    35, "STRENGTH", 3, "job-knight"));
            case "GUARD" -> out.add(new OfferTemplate(
                    "KILL", "SKELETON", 5,
                    "야간 순찰을 도와라. 스켈레톤 5마리 처리.",
                    18, "AGILITY", 1, "job-guard"));
            case "INNKEEPER" -> out.add(new OfferTemplate(
                    "GATHER", "WHEAT", 24,
                    "여관 식량이 부족하다. 밀 24다발만.",
                    16, "CHARISMA", 1, "job-inn"));
            default -> {} // 일반 villager는 욕구만
        }

        // ─── 성격 기반 (성격이 극단적일 때) ───
        if (p.get(Personality.Trait.GREED) >= 60) {
            out.add(new OfferTemplate("GATHER", "DIAMOND", 4,
                    "다이아 4개를 가져와라. 보상은 후하게.",
                    35, "LUCK", 3, "trait-greed"));
        }
        if (p.get(Personality.Trait.EMPATHY) >= 60) {
            out.add(new OfferTemplate("GATHER", "GOLDEN_APPLE", 2,
                    "병자가 있다. 황금사과 2개로 살리고 싶다.",
                    30, "MENTAL", 3, "trait-empathy"));
        }
        if (p.get(Personality.Trait.AMBITION) >= 70) {
            out.add(new OfferTemplate("EXPLORE", "", 4,
                    "내 야망을 위해 정찰. 4개 지역 정보.",
                    30, "CHARISMA", 3, "trait-ambition"));
        }
        if (p.get(Personality.Trait.CURIOSITY) >= 70) {
            out.add(new OfferTemplate("GATHER", "EXPERIENCE_BOTTLE", 8,
                    "신비한 경험병 8개에 흥미가 있다.",
                    25, "INTELLIGENCE", 3, "trait-curiosity"));
        }
        if (p.get(Personality.Trait.AGGRESSION) >= 70) {
            out.add(new OfferTemplate("KILL", "RAVAGER", 1,
                    "분노를 풀고 싶다. 라바저 1체를 갈가리.",
                    45, "STRENGTH", 5, "trait-aggression"));
        }

        return out;
    }

    private void registerOffer(RebornNpc npc, OfferTemplate t, long now) {
        try {
            var qp = Bukkit.getPluginManager().getPlugin("RebornQuest");
            if (qp == null) return;
            Object registry = qp.getClass().getMethod("registry").invoke(qp);
            String qid = "npc_" + npc.id + "_" + (now / 1000);
            Class<?> questCls = Class.forName("kr.reborn.quest.engine.Quest");
            Object quest = questCls.getConstructor(String.class, String.class, String.class,
                            String.class, int.class, String.class, String.class, java.util.Map.class)
                    .newInstance(qid,
                            "[" + npc.displayName + "] " + t.desc,
                            t.type, t.target, t.amount,
                            npc.world == null ? "" : npc.world.name(),
                            null,
                            java.util.Map.of(
                                    "favor", t.favor,
                                    "stats", java.util.Map.of(t.rewardStat, t.rewardAmount),
                                    "npcId", npc.id,
                                    "tag", t.tag));
            registry.getClass().getMethod("register", questCls).invoke(registry, quest);
            npc.pendingQuestOffer = qid;
            npc.pendingQuestOfferAt = now;
            npc.aiData.put("lastQuestOffer", now);
            npc.aiData.put("lastQuestTag", t.tag);
        } catch (Throwable ignored) {}
    }

    /**
     * 만료 시 처벌:
     *   1) 발주 NPC 본인 STATUS -2 (의뢰 실패 자존감 손상).
     *   2) NPC 30블록 내 온라인 플레이어 favor -10 (가까이 있었으면서 무시한 죄).
     *   3) NPC의 Memory에 그 플레이어 OPPOSED_ME 5 기록 — 다음 만남 때 시선 차가워짐.
     *
     * 거리 기반이라 한 명도 처벌 안 받을 수 있으나, 그건 의뢰가 시골에서 났다는 뜻.
     * "무시" 처벌이 도시 인구 밀집 NPC에서 더 강하게 작동하는 자연스러운 흐름.
     */
    private void expirePending(RebornNpc npc) {
        String offerId = npc.pendingQuestOffer;
        npc.pendingQuestOffer = null;
        npc.pendingQuestOfferAt = 0;
        if (npc.soul != null) {
            npc.soul.needs.add(Needs.Kind.STATUS, -2);
        }
        if (npc.location == null || npc.location.getWorld() == null) return;
        int penalized = 0;
        for (org.bukkit.entity.Player p : npc.location.getWorld().getPlayers()) {
            try {
                if (p.getLocation().distance(npc.location) <= 30) {
                    npc.relations.addPlayer(p.getUniqueId(), -10);
                    if (npc.soul != null) {
                        npc.soul.memory.record(p.getUniqueId().toString(),
                                kr.reborn.npc.soul.Memory.Kind.OPPOSED_ME, 5,
                                "내 의뢰 " + (offerId == null ? "" : offerId) + " 무시함");
                    }
                    penalized++;
                }
            } catch (Throwable ignored) {}
        }
        if (penalized > 0) {
            try {
                org.bukkit.Bukkit.broadcastMessage("§8[NPC] §f"
                        + npc.displayName + " §7이(가) 의뢰 무시에 실망 ("
                        + penalized + "명 호감도 감소)");
            } catch (Throwable ignored) {}
        }
    }

    public String pendingOfferOf(RebornNpc npc) {
        return npc.pendingQuestOffer;
    }

    /**
     * 퀘스트 완료 시 — 발주 NPC의 pendingQuestOffer 해제 + 호감도 보상.
     *
     * 이전엔 24h TTL 만료까지 NPC가 의뢰 1건에 묶여 새 의뢰 발급 불가 — 완료해도 동일.
     * 이제 완료가 곧바로 NPC를 해방시켜 다음 의뢰 생성을 허용한다.
     */
    @EventHandler
    public void onQuestComplete(RebornQuestCompleteEvent e) {
        String questId = e.questId();
        if (questId == null || !questId.startsWith("npc_")) return;
        // npc_ prefix인 의뢰만 — 자율 의뢰 식별.
        for (RebornNpc npc : plugin.registry().all()) {
            if (questId.equals(npc.pendingQuestOffer)) {
                npc.pendingQuestOffer = null;
                npc.pendingQuestOfferAt = 0;
                if (npc.soul != null) {
                    // 의뢰 완수 — 발주 NPC가 만족, STATUS·ACHIEVEMENT 욕구 충족.
                    npc.soul.needs.add(Needs.Kind.STATUS, 8);
                    npc.soul.needs.add(Needs.Kind.ACHIEVEMENT, 5);
                }
                return;
            }
        }
    }
}
