package kr.reborn.stat.growth.impl;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import kr.reborn.stat.growth.GrowthStrategy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 해양제국 성장: 바다에서만, 다이빙·해전·진주 채집.
 *
 * 외부 호출:
 *   onDive(p, depth): 깊이별 해양력 누적 (최대 100m)
 *   onPearl(p, quality): 진주 채집 — quality 1~5
 *   onSeaBattle(p, victimLevel): 해상 전투 승리
 *   onShipCapture(p): 적선 나포 — 명성 + 해양력
 *
 * 마일스톤:
 *   해양력 1000: 항해사 단계 (+ WATER_BREATHING 영구)
 *   해양력 5000: 선장 단계
 *   해양력 20000: 해왕 후보 단계
 */
public final class OceanGrowth implements GrowthStrategy {

    private static final String NS = "RebornStat.ocean";

    /** uuid → 다이빙 누적 미터 */
    private final Map<UUID, Double> diveMeters = new ConcurrentHashMap<>();
    /** uuid → 진주 수 */
    private final Map<UUID, Integer> pearls = new ConcurrentHashMap<>();
    /** uuid → 적선 나포 횟수 */
    private final Map<UUID, Integer> captures = new ConcurrentHashMap<>();
    /** uuid → 적용된 단계 */
    private final Map<UUID, Integer> stage = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> loaded = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** uuid → 현재 활동 중인 항구 id. */
    private final Map<UUID, String> activePort = new ConcurrentHashMap<>();
    private final PortRegistry ports = new PortRegistry();

    public PortRegistry ports() { return ports; }

    private void ensureLoaded(UUID p) {
        if (loaded.add(p)) {
            diveMeters.put(p, RebornCore.get().kv().getDouble(NS, p, "dive", 0.0));
            pearls.put(p, RebornCore.get().kv().getInt(NS, p, "pearls", 0));
            captures.put(p, RebornCore.get().kv().getInt(NS, p, "captures", 0));
            stage.put(p, RebornCore.get().kv().getInt(NS, p, "stage", -1));
        }
    }

    @Override public WorldKey world() { return WorldKey.OCEAN; }

    private boolean atSea(Player p) {
        Material m = p.getLocation().getBlock().getType();
        return m == Material.WATER || p.isInWater();
    }

    @Override
    public void onMonsterKill(Player p, PlayerData d, double mobLevel) {
        if (!atSea(p)) return;
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.OCEAN_POWER, 1.0, "sea-kill");
        // 소속 제국을 위한 해상 전투 — 후원 제국 평판 미세 누적.
        String patron = patronEmpire(p.getUniqueId());
        if (patron != null) gainEmpireFavor(p, patron, 1);
        checkStage(p);
    }

    @Override
    public void onQuestComplete(Player p, PlayerData d, double weight) {
        double power = 5 * weight;
        // 후원 제국 동맹 단계면 항해 지원 — 해양력 보너스 (기획서 5-13 동맹 효과).
        String patron = patronEmpire(p.getUniqueId());
        if (patron != null) {
            int tier = empireTier(empireReputation(p.getUniqueId(), patron));
            if (tier >= 4) {
                power *= 1.20;  // 제국 후원: 보급·항로 지원
                if (Rand.chance(0.25)) {
                    Msg.send(p, "&3[" + patron + "] §7제국 후원 — 항해 보급 지원.");
                }
            }
            // 활동 항구가 후원 제국 점령지면 추가 +10% — 점령지 항만 우대.
            String port = activePortOf(p.getUniqueId());
            if (port != null && patron.equals(ports.rulerOf(port))) {
                power *= 1.10;
            }
            gainEmpireFavor(p, patron, (int) Math.round(8 * weight));
        }
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.OCEAN_POWER, power, "voyage");
        checkStage(p);
    }

    /** 활동 중인 항구 id. null이면 정박 안 함. */
    public String activePortOf(UUID p) {
        String cached = activePort.get(p);
        if (cached != null) return cached;
        String stored = RebornCore.get().kv().get(NS, p, "port");
        if (stored != null && !stored.isEmpty()) {
            activePort.put(p, stored);
            return stored;
        }
        return null;
    }

    public boolean setActivePort(Player p, String portId) {
        if (!OceanPort.isPort(portId)) return false;
        activePort.put(p.getUniqueId(), portId);
        RebornCore.get().kv().put(NS, p.getUniqueId(), "port", portId);
        return true;
    }

    public void clearActivePort(Player p) {
        activePort.remove(p.getUniqueId());
        RebornCore.get().kv().remove(NS, p.getUniqueId(), "port");
    }

    @Override
    public void onMeditate(Player p, PlayerData d, double quality) {
        if (!atSea(p)) {
            Msg.warn(p, "해양력 수련은 바다 위에서만 가능합니다.");
            return;
        }
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.OCEAN_POWER, 4 * quality, "tide-meditate");
        // 다이빙 보너스
        double y = p.getLocation().getY();
        if (y < 30) {
            // 깊은 곳일수록 보너스
            double depthBonus = (30 - y) * 0.5 * quality;
            RebornCore.get().api().addStat(p.getUniqueId(),
                    StatType.OCEAN_POWER, depthBonus, "deep-meditate");
        }
        checkStage(p);
    }

    public void onDive(Player p, double meters) {
        ensureLoaded(p.getUniqueId());
        double cur = diveMeters.merge(p.getUniqueId(), meters, Double::sum);
        RebornCore.get().kv().putDouble(NS, p.getUniqueId(), "dive", cur);
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.OCEAN_POWER, meters * 0.3, "dive");
        if (cur >= 1000 && cur - meters < 1000) {
            Msg.send(p, "&b1000m 다이빙 누적 — 해양력 +500");
            RebornCore.get().api().addStat(p.getUniqueId(),
                    StatType.OCEAN_POWER, 500, "dive-1000m");
        }
    }

    public void onPearl(Player p, int quality) {
        ensureLoaded(p.getUniqueId());
        int n = pearls.merge(p.getUniqueId(), 1, Integer::sum);
        RebornCore.get().kv().putInt(NS, p.getUniqueId(), "pearls", n);
        double bonus = 10 * quality;
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.OCEAN_POWER, bonus, "pearl");
        Msg.send(p, "&b진주 채집 (Q" + quality + ") — 해양력 +" + bonus + " §7(총 " + n + "개)");
        if (n % 10 == 0) {
            RebornCore.get().api().addStat(p.getUniqueId(),
                    StatType.LUCK, 5, "pearl-10");
            Msg.send(p, "&6진주 10개 — 행운 +5");
        }
    }

    public void onSeaBattle(Player p, double victimLevel) {
        if (!atSea(p)) return;
        double bonus = 5 + victimLevel * 0.1;
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.OCEAN_POWER, bonus, "sea-battle");
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.STRENGTH, 0.5, "sea-battle");
    }

    public void onShipCapture(Player p) {
        ensureLoaded(p.getUniqueId());
        int n = captures.merge(p.getUniqueId(), 1, Integer::sum);
        RebornCore.get().kv().putInt(NS, p.getUniqueId(), "captures", n);
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.OCEAN_POWER, 100, "ship-capture");
        RebornCore.get().api().addStat(p.getUniqueId(),
                StatType.CHARISMA, 3, "ship-capture");
        // 나포는 후원 제국에 대한 큰 무공 — 평판 +15 (라이벌 제국 -7).
        String patron = patronEmpire(p.getUniqueId());
        if (patron != null) gainEmpireFavor(p, patron, 15);
        Bukkit.broadcastMessage("§3§l[해전] §f" + p.getName()
                + " §7가 적선을 나포 §6(총 " + n + "척) §7- 해양력 +100");
    }

    private void checkStage(Player p) {
        ensureLoaded(p.getUniqueId());
        double ki = RebornCore.get().api().getStat(p.getUniqueId(), StatType.OCEAN_POWER);
        int newStage = ki >= 20000 ? 3 : ki >= 5000 ? 2 : ki >= 1000 ? 1 : 0;
        int prev = stage.getOrDefault(p.getUniqueId(), -1);
        if (newStage > prev) {
            stage.put(p.getUniqueId(), newStage);
            RebornCore.get().kv().putInt(NS, p.getUniqueId(), "stage", newStage);
            String label = switch (newStage) {
                case 1 -> "항해사";
                case 2 -> "선장";
                case 3 -> "해왕 후보";
                default -> "범인";
            };
            Bukkit.broadcastMessage("§3§l[해양] §f" + p.getName() + " §7가 §6" + label + " §7단계!");
            if (newStage == 1) {
                try {
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.WATER_BREATHING,
                            Integer.MAX_VALUE, 0, true, false));
                } catch (Throwable ignored) {}
            }
            if (newStage == 3) {
                RebornCore.get().api().addStat(p.getUniqueId(),
                        StatType.OCEAN_POWER, 5000, "sea-king-candidate");
            }
        }
    }

    public int stageOf(UUID p) { ensureLoaded(p); return Math.max(0, stage.getOrDefault(p, 0)); }
    public int pearlsOf(UUID p) { ensureLoaded(p); return pearls.getOrDefault(p, 0); }

    // ───────────────────────── 7대 해양 제국 평판 ─────────────────────────
    /**
     * 기획서 5-13: 7대 해양 제국.
     * 각 제국 평판(-1000~+1000) — 단계별 효과:
     *   +500↑ Citizen — 항구 무료, 전용 거래
     *   +100↑ Allied — 항구 50% 할인
     *   -200↓ Hostile — 해당 영해 진입 시 공격
     *   -500↓ Enemy — 현상수배, 함대 추격
     */
    public static final String[] EMPIRES = {
            "AQUARION",          // 아쿠아리온 — 정규 해군 / 질서 수호
            "CORAL_UNION",       // 코럴 연합 — 무역
            "KRAKEN_THEOCRACY",  // 크라켄 신전국 — 심해 종교
            "PEARL_ABYSS",       // 인어 왕국 펄 아비스
            "FREE_SEA",          // 자유해 — 해적
            "STORM_EMPIRE",      // 폭풍 제국 — 군사
            "GHOST_FLEET"        // 망자의 해류 — 언데드
    };

    /** 정의·자유 진영 vs 해적·언데드 — 한쪽 우호 시 다른 쪽 감소. */
    private static final Map<String, String> EMPIRE_RIVALS = Map.of(
            "AQUARION", "FREE_SEA",          // 해군 vs 해적
            "FREE_SEA", "AQUARION",
            "CORAL_UNION", "STORM_EMPIRE",   // 무역 vs 군사
            "STORM_EMPIRE", "CORAL_UNION",
            "KRAKEN_THEOCRACY", "PEARL_ABYSS", // 신정 vs 인어 (심해 vs 인어계)
            "PEARL_ABYSS", "KRAKEN_THEOCRACY",
            "GHOST_FLEET", "AQUARION"        // 언데드는 해군 영원의 적
    );

    private static final String EMPIRE_NS = "RebornStat.ocean.empire";

    public int empireReputation(UUID p, String empireId) {
        return RebornCore.get().kv().getInt(EMPIRE_NS, p, empireId, 0);
    }

    public void gainEmpireFavor(Player p, String empireId, int delta) {
        if (!isEmpire(empireId)) return;
        int cur = empireReputation(p.getUniqueId(), empireId);
        int next = Math.max(-1000, Math.min(1000, cur + delta));
        RebornCore.get().kv().putInt(EMPIRE_NS, p.getUniqueId(), empireId, next);
        if (delta > 0) {
            String rival = EMPIRE_RIVALS.get(empireId);
            if (rival != null) {
                int rivalCur = empireReputation(p.getUniqueId(), rival);
                int rivalNext = Math.max(-1000, rivalCur - (delta / 2));
                RebornCore.get().kv().putInt(EMPIRE_NS, p.getUniqueId(), rival, rivalNext);
            }
            // 활동 중인 항구가 있으면 제국 영향력 누적 (항구 점령전).
            String port = activePortOf(p.getUniqueId());
            if (port != null) {
                ports.addInfluence(port, empireId, delta);
            }
        }
        notifyEmpireStatus(p, empireId, cur, next);
    }

    private void notifyEmpireStatus(Player p, String empireId, int prev, int cur) {
        int prevTier = empireTier(prev);
        int curTier = empireTier(cur);
        if (prevTier == curTier) return;
        String[] labels = {"적", "적대", "냉랭", "중립", "동맹", "시민"};
        String label = labels[Math.max(0, Math.min(labels.length - 1, curTier))];
        Msg.send(p, "&3[" + empireId + "] §f관계: §6" + label + " §7(" + cur + ")");
        if (curTier >= 4) {
            Bukkit.broadcastMessage("§3§l[" + empireId + "] §f" + p.getName()
                    + " §7가 §6" + label + " §7관계 도달.");
        }
    }

    public int empireTier(int rep) {
        if (rep <= -500) return 0;
        if (rep <= -200) return 1;
        if (rep < 100) return 2;
        if (rep < 300) return 3;
        if (rep < 500) return 4;
        return 5;
    }

    public boolean isEmpire(String id) {
        if (id == null) return false;
        for (String c : EMPIRES) if (c.equals(id)) return true;
        return false;
    }

    /** 해상 임무 보상 — 종류별 평판. */
    public void onEmpireMission(Player p, String empireId, String missionType) {
        int amount = switch (missionType) {
            case "escort"   -> 20;   // 호송
            case "naval"    -> 60;   // 해전 참가
            case "explore"  -> 35;   // 신항로 발견
            case "pirate"   -> 100;  // 해적 처치 (대형 임무)
            case "betray"   -> -150; // 배신
            default         -> 10;
        };
        gainEmpireFavor(p, empireId, amount);
    }

    public Map<String, Integer> allEmpireReputations(UUID p) {
        Map<String, Integer> out = new java.util.LinkedHashMap<>();
        for (String c : EMPIRES) out.put(c, empireReputation(p, c));
        return out;
    }

    /**
     * 후원 제국 — 평판이 가장 높은(>0) 제국. /empire join 또는 해상 임무로 형성.
     * 후원 제국이 있어야 전투·항해가 평판을 누적하고 동맹 단계 보급을 받는다.
     */
    public String patronEmpire(UUID p) {
        String best = null;
        int bestRep = 0;
        for (String c : EMPIRES) {
            int rep = empireReputation(p, c);
            if (rep > bestRep) { bestRep = rep; best = c; }
        }
        return best;
    }
}
