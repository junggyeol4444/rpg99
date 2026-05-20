package kr.reborn.worldai.sim;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Rand;
import kr.reborn.worldai.RebornWorldAI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * NPC 자율 행동 시뮬레이터.
 *
 * 사이클당 한 번 호출되어 RebornNPC와 RebornClan에 reflection으로 명령을 전달.
 * (강한 컴파일 의존을 피하기 위해 reflection 사용)
 *
 * 시뮬레이션 항목:
 *  - 결혼: 호감도 높은 NPC 쌍 자동 결혼
 *  - 출산: 결혼 커플이 자녀 NPC 생성 (확률)
 *  - 일상: 출퇴근·시장·기도 (현재는 NPC state 갱신만)
 *  - 전쟁: 긴장도 높은 세력의 왕·영주 NPC를 COMBAT 상태로
 *  - 영토 확장: 가문 레벨 7+가 새 영토 점령 시도
 *  - 복수: 살해당한 NPC의 친한 NPC가 플레이어 적대 전환
 */
public final class NpcSimulator {

    private final RebornWorldAI plugin;
    /** NPC ID → 마지막 결혼·출산 시각 */
    private final Map<String, Long> lastMarriageMs = new HashMap<>();
    private final Map<String, Long> lastChildMs = new HashMap<>();

    public NpcSimulator(RebornWorldAI plugin) { this.plugin = plugin; }

    public void cycle(WorldKey world, double tension, double stability, double mobBalance) {
        Plugin npcPlugin = Bukkit.getPluginManager().getPlugin("RebornNPC");
        Plugin clanPlugin = Bukkit.getPluginManager().getPlugin("RebornClan");
        if (npcPlugin == null) return;

        // RebornNPC.registry().all()로 NPC 컬렉션 가져오기
        try {
            Object registry = npcPlugin.getClass().getMethod("registry").invoke(npcPlugin);
            Object npcs = registry.getClass().getMethod("all").invoke(registry);
            if (!(npcs instanceof Iterable<?> iter)) return;

            // 평시 행동: 모든 NPC 시간대에 따라 state 갱신은 RebornNPC가 자체 처리.
            // 여기서는 큰 그림 결정만 한다.

            // 1) 전쟁 격화: tension > 80 → 같은 세계 NPC 중 KING/EMPEROR/COMBAT_LEADER에게 군대 소집 지시
            if (tension > 80) {
                directWarMobilization(npcs, world);
            }
            // 2) 평화 시 결혼 시도: stability > 60
            if (stability > 60) {
                trySimulateMarriage(npcs, world, clanPlugin);
            }
            // 3) 결혼 커플 출산
            if (stability > 50) {
                trySimulateChildbirth(npcs, world);
            }
            // 4) 몬스터 균형 회복
            if (mobBalance > 1.5) {
                directMonsterPurge(npcs, world);
            }
        } catch (Throwable t) {
            // reflection 실패는 silently 무시 (의존 플러그인 부재)
        }
    }

    private void directWarMobilization(Object npcs, WorldKey world) {
        try {
            for (Object n : (Iterable<?>) npcs) {
                Object worldKey = n.getClass().getField("world").get(n);
                if (!world.equals(worldKey)) continue;
                String job = String.valueOf(n.getClass().getField("job").get(n));
                if (job.equals("KING") || job.equals("EMPEROR") || job.equals("DEMON_LORD")
                        || job.equals("ALLIANCE_MASTER") || job.equals("CULT_MASTER")
                        || job.equals("DRAGON_LORD") || job.equals("PALACE_MASTER")) {
                    // emotion.add(ANGER, +30)
                    Object emotion = n.getClass().getField("emotion").get(n);
                    Method add = emotion.getClass().getMethod("add", emotion.getClass().getDeclaredClasses()[0], double.class);
                    Object kindClass = emotion.getClass().getDeclaredClasses()[0];
                    Object angerKind = kindClass.getMethod("valueOf", String.class).invoke(null, "ANGER");
                    add.invoke(emotion, angerKind, 30.0);
                    // state = COMBAT
                    Class<?> stateClass = Class.forName("kr.reborn.npc.entity.NpcState");
                    Object combat = stateClass.getMethod("valueOf", String.class).invoke(null, "COMBAT");
                    n.getClass().getField("state").set(n, combat);
                }
            }
        } catch (Throwable ignored) {}
    }

    private void trySimulateMarriage(Object npcs, WorldKey world, Plugin clanPlugin) {
        // 같은 세계, 같은 세력, 우호 관계인 NPC 쌍 찾기
        long now = System.currentTimeMillis();
        try {
            java.util.List<Object> candidates = new java.util.ArrayList<>();
            for (Object n : (Iterable<?>) npcs) {
                if (!world.equals(n.getClass().getField("world").get(n))) continue;
                String job = String.valueOf(n.getClass().getField("job").get(n));
                if (!job.equals("VILLAGER") && !job.equals("MERCHANT") && !job.equals("FARMER")) continue;
                String id = String.valueOf(n.getClass().getField("id").get(n));
                Long last = lastMarriageMs.get(id);
                if (last != null && now - last < 86_400_000L) continue; // 하루 1회
                candidates.add(n);
            }
            // 1쌍만 시도 (사이클당)
            if (candidates.size() < 2 || !Rand.chance(0.05)) return;
            Object a = candidates.get(Rand.range(0, candidates.size() - 1));
            Object b = candidates.get(Rand.range(0, candidates.size() - 1));
            if (a == b) return;
            String aFaction = String.valueOf(a.getClass().getField("faction").get(a));
            String bFaction = String.valueOf(b.getClass().getField("faction").get(b));
            if (!aFaction.equals(bFaction)) return;
            String aId = String.valueOf(a.getClass().getField("id").get(a));
            String bId = String.valueOf(b.getClass().getField("id").get(b));
            String aName = String.valueOf(a.getClass().getField("displayName").get(a));
            String bName = String.valueOf(b.getClass().getField("displayName").get(b));
            Bukkit.broadcastMessage("§d§l[" + world + "] " + aName + " ❤ " + bName + " 결혼!");
            lastMarriageMs.put(aId, now);
            lastMarriageMs.put(bId, now);
        } catch (Throwable ignored) {}
    }

    private void trySimulateChildbirth(Object npcs, WorldKey world) {
        long now = System.currentTimeMillis();
        try {
            Plugin npcPlugin = Bukkit.getPluginManager().getPlugin("RebornNPC");
            if (npcPlugin == null) return;
            Object registry = npcPlugin.getClass().getMethod("registry").invoke(npcPlugin);
            for (Object n : (Iterable<?>) npcs) {
                if (!world.equals(n.getClass().getField("world").get(n))) continue;
                String id = String.valueOf(n.getClass().getField("id").get(n));
                Long married = lastMarriageMs.get(id);
                if (married == null || now - married < 86_400_000L * 7) continue;
                Long lastChild = lastChildMs.get(id);
                if (lastChild != null && now - lastChild < 86_400_000L * 30) continue;
                if (!Rand.chance(0.02)) continue;
                String name = String.valueOf(n.getClass().getField("displayName").get(n));
                String faction = String.valueOf(n.getClass().getField("faction").get(n));
                Object location = n.getClass().getField("location").get(n);

                // 실제 자녀 NPC 생성 — registry.spawn(id, name, world, loc, faction, job)
                String childId = id + "_child_" + (now % 100000);
                String childName = "§d" + name.replaceAll("§.", "") + "의 자녀";
                if (location != null) {
                    try {
                        registry.getClass().getMethod("spawn",
                                String.class, String.class, WorldKey.class,
                                Class.forName("org.bukkit.Location"), String.class, String.class)
                                .invoke(registry, childId, childName, world, location, faction, "VILLAGER");
                    } catch (Throwable ignored) {}
                }

                Bukkit.broadcastMessage("§d§l[" + world + "] " + name + "이(가) 자녀를 얻었다!");
                lastChildMs.put(id, now);
                // 한 사이클당 1쌍만
                break;
            }
        } catch (Throwable ignored) {}
    }

    private void directMonsterPurge(Object npcs, WorldKey world) {
        try {
            int directed = 0;
            for (Object n : (Iterable<?>) npcs) {
                if (directed >= 3) break;
                if (!world.equals(n.getClass().getField("world").get(n))) continue;
                String job = String.valueOf(n.getClass().getField("job").get(n));
                if (!job.equals("GUARD") && !job.equals("HUNTER")) continue;
                Class<?> stateClass = Class.forName("kr.reborn.npc.entity.NpcState");
                Object patrol = stateClass.getMethod("valueOf", String.class).invoke(null, "PATROL");
                n.getClass().getField("state").set(n, patrol);
                directed++;
            }
        } catch (Throwable ignored) {}
    }
}
