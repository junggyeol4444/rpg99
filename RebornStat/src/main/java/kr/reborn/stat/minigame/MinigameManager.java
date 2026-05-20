package kr.reborn.stat.minigame;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.StatType;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import kr.reborn.stat.RebornStat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 운기조식·천겁·깨달음 미니게임을 호스팅한다.
 *
 * 천겁: 실제 낙뢰가 1초 전 경고 파티클 → 낙하. 경지에 따라 발수·간격·동시발수 변동.
 *       회피는 플레이어 위치 vs 낙뢰 위치(2.5블록) 거리. 피격 시 폭발 데미지+감지.
 *
 * 깨달음: 내면의 분신(플레이어 1.2배 스탯) 가상 전투 시뮬레이션 + 3개 철학 질문.
 *         질문은 채팅에 표시되고 30초 내 응답해야 함.
 */
public final class MinigameManager {

    private final RebornStat plugin;
    private final ConcurrentHashMap<UUID, MeditationSession> meditation = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, TribulationSession> tribulation = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, EnlightenmentSession> enlightenment = new ConcurrentHashMap<>();

    public MinigameManager(RebornStat plugin) { this.plugin = plugin; }

    // ============================================================
    // 운기조식 — 바닐라 입력 (Sneak/Sprint/Jump) 시퀀스
    // ============================================================
    public void startMeditation(Player p, int tier) {
        var c = plugin.getConfig();
        int keys = Math.min(c.getInt("minigame.meditation.max-keys", 8),
                c.getInt("minigame.meditation.base-keys", 4) + tier);
        double seconds = Math.max(c.getInt("minigame.meditation.min-time", 3),
                c.getInt("minigame.meditation.base-time", 5) - tier * 0.5);
        List<String> seq = new ArrayList<>();
        // Sneak(S), Sprint(R), Jump(J) — 바닐라 입력으로 매핑
        String[] actions = {"S", "R", "J"};
        for (int i = 0; i < keys; i++) seq.add(actions[Rand.range(0, actions.length - 1)]);
        meditation.put(p.getUniqueId(), new MeditationSession(seq,
                System.currentTimeMillis() + (long)(seconds * 1000), tier));
        Msg.send(p, "&e운기조식 시작! 다음 동작 시퀀스를 수행하라 (" + (int)seconds + "초):");
        StringBuilder pretty = new StringBuilder("&f");
        for (String s : seq) {
            switch (s) {
                case "S" -> pretty.append("&7[웅크리기] ");
                case "R" -> pretty.append("&a[달리기] ");
                case "J" -> pretty.append("&b[점프] ");
            }
        }
        Msg.send(p, pretty.toString());
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1f, 1f);
    }

    /** Sneak/Sprint/Jump 이벤트에서 호출. action: "S" | "R" | "J" */
    public void recordMeditationInput(Player p, String action) {
        MeditationSession s = meditation.get(p.getUniqueId());
        if (s == null) return;
        if (System.currentTimeMillis() > s.deadline) {
            meditation.remove(p.getUniqueId());
            Msg.error(p, "시간 초과 — 주화입마 위험!");
            if (Rand.chance(0.15)) applyQiDeviation(p);
            return;
        }
        s.inputs.add(action);
        if (s.inputs.size() >= s.sequence.size()) {
            evaluateMeditation(p, s);
            meditation.remove(p.getUniqueId());
        }
    }

    private void evaluateMeditation(Player p, MeditationSession s) {
        int matches = 0;
        for (int i = 0; i < s.sequence.size(); i++) {
            if (i < s.inputs.size() && s.sequence.get(i).equals(s.inputs.get(i))) matches++;
        }
        double q = (double) matches / s.sequence.size();
        if (q >= 0.99) {
            int streak = s.tier > 0 ? s.tier : 1;
            Msg.send(p, "&a완벽 성공! 내공 +" + (5 * streak));
            plugin.growth().meditate(p, 1.0 * streak);
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.5f);
        } else {
            Msg.warn(p, "부분 성공 (" + (int)(q * 100) + "%) 내공 +" + (int)(5 * q));
            plugin.growth().meditate(p, q);
            if (q < 0.3 && Rand.chance(0.1)) applyQiDeviation(p);
        }
    }

    public void onMeditationInput(Player p, String input) {
        MeditationSession s = meditation.remove(p.getUniqueId());
        if (s == null) return;
        if (System.currentTimeMillis() > s.deadline) {
            Msg.error(p, "시간 초과 — 주화입마 위험!");
            if (Rand.chance(0.15)) {
                Msg.error(p, "&5&l[주화입마]");
                applyQiDeviation(p);
            }
            return;
        }
        String expected = String.join("", s.sequence).replaceAll(" ", "");
        String got = input.replaceAll("\\s+", "");
        if (got.equalsIgnoreCase(expected)) {
            // 연속 완벽 보너스 추적
            int streak = s.tier > 0 ? s.tier : 1;
            Msg.send(p, "&a완벽 성공! 내공 +" + (5 * streak));
            plugin.growth().meditate(p, 1.0 * streak);
            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.5f);
        } else {
            int matches = 0;
            for (int i = 0; i < Math.min(got.length(), expected.length()); i++) {
                if (Character.toLowerCase(got.charAt(i)) == Character.toLowerCase(expected.charAt(i))) matches++;
            }
            double q = (double) matches / expected.length();
            Msg.warn(p, "부분 성공 (" + (int)(q*100) + "%) 내공 +" + (int)(5 * q));
            plugin.growth().meditate(p, q);
            if (q < 0.3 && Rand.chance(0.1)) applyQiDeviation(p);
        }
    }

    private void applyQiDeviation(Player p) {
        // RebornCurse가 있으면 hook (간이로는 정신력 즉시 감소만)
        RebornCore.get().api().addStat(p.getUniqueId(), StatType.MENTAL, -5, "qi-deviation");
        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_HURT, 1f, 0.5f);
    }

    // ============================================================
    // 천겁 — 실제 낙뢰 + 위치 추적 회피
    // ============================================================
    public void startTribulation(Player p, String tierName) {
        var c = plugin.getConfig();
        String tag = tierTag(tierName);
        int bolts = c.getInt("minigame.tribulation.bolts-by-tier." + tag, 10);
        double interval = Math.max(c.getDouble("minigame.tribulation.interval-min", 0.3),
                2.0 - tag.equals("higher") ? 1.5 : (tag.equals("dragon") ? 1.7 : 1.0));
        int simultaneous = "dragon".equals(tag) ? 5 : ("higher".equals(tag) ? 3 : 1);

        TribulationSession s = new TribulationSession(bolts, simultaneous, interval, tierName);
        tribulation.put(p.getUniqueId(), s);
        Bukkit.broadcastMessage("§5§l[천겁] §f" + p.getName() + "이(가) 천겁을 맞이한다!");
        p.sendTitle("§5§l천겁", "§f낙뢰 " + bolts + "발 — 회피하라", 10, 80, 20);
        scheduleBolts(p, s);
    }

    private void scheduleBolts(Player p, TribulationSession s) {
        long step = Math.max(8L, (long) (s.intervalSeconds * 20));
        for (int round = 0; round < s.totalBolts; round += s.simultaneous) {
            final int currentRound = round;
            RebornCore.get().scheduler().runTaskLater(() -> {
                if (!p.isOnline() || !tribulation.containsKey(p.getUniqueId())) return;
                int boltsThisRound = Math.min(s.simultaneous, s.totalBolts - currentRound);
                for (int i = 0; i < boltsThisRound; i++) fireBolt(p, s);
                if (currentRound + boltsThisRound >= s.totalBolts) {
                    // 마지막 라운드 — 다음 틱에 결과 처리
                    RebornCore.get().scheduler().runTaskLater(() -> finishTribulation(p, s), 40L);
                }
            }, (long)(currentRound / Math.max(1, s.simultaneous) * step));
        }
    }

    private void fireBolt(Player p, TribulationSession s) {
        Location target = p.getLocation().add(Rand.rangeD(-6, 6), 0, Rand.rangeD(-6, 6));
        // 1초 전 경고 파티클
        for (int t = 0; t < 20; t += 5) {
            final int tt = t;
            RebornCore.get().scheduler().runTaskLater(() -> {
                if (!p.isOnline()) return;
                target.getWorld().spawnParticle(Particle.FLAME, target, 30, 0.5, 0.2, 0.5, 0);
                p.playSound(target, Sound.BLOCK_FIRE_AMBIENT, 0.5f, 2f);
            }, tt);
        }
        // 1초 후 실제 낙하
        RebornCore.get().scheduler().runTaskLater(() -> {
            if (!p.isOnline()) return;
            target.getWorld().strikeLightning(target);
            // 회피 판정: 플레이어가 2.5블록 안에 있으면 피격
            double dist = p.getLocation().distance(target);
            if (dist < 2.5) {
                s.hits++;
                p.damage(Math.max(2, p.getMaxHealth() * 0.05));
                Msg.error(p, "&c⚡ 피격! (" + s.hits + "/" + s.tolerance + ")");
            }
        }, 20L);
    }

    private void finishTribulation(Player p, TribulationSession s) {
        tribulation.remove(p.getUniqueId());
        s.tolerance = plugin.getConfig().getInt("minigame.tribulation.success-tolerance", 3);
        if (s.hits == 0) {
            Bukkit.broadcastMessage("§6§l[천겁 완벽 통과] §f" + p.getName() + "!");
            p.sendTitle("§6§l천겁 통과!", "§f경지 돌파", 10, 80, 20);
            RebornCore.get().api().addStat(p.getUniqueId(), StatType.IMMORTAL_KI, 200, "tribulation-perfect");
        } else if (s.hits <= s.tolerance) {
            Msg.send(p, "&e천겁 통과 (피격 " + s.hits + "회) — 약간의 손실");
            RebornCore.get().api().addStat(p.getUniqueId(), StatType.IMMORTAL_KI, 100, "tribulation-pass");
            RebornCore.get().api().addStat(p.getUniqueId(), StatType.MENTAL, -3, "tribulation-pass");
        } else {
            Bukkit.broadcastMessage("§4§l[천겁 실패] §f" + p.getName() + " (피격 " + s.hits + "회)");
            p.sendTitle("§4§l실패", "§7스탯 대량 손실", 10, 80, 20);
            RebornCore.get().api().addStat(p.getUniqueId(), StatType.IMMORTAL_KI, -100, "tribulation-fail");
            RebornCore.get().api().addStat(p.getUniqueId(), StatType.MENTAL, -20, "tribulation-fail");
            // 재기 불능 위험: 사망 처리
            if (s.hits > s.totalBolts / 2) {
                p.setHealth(0);
                Bukkit.broadcastMessage("§4§l" + p.getName() + "이(가) 천겁에 의해 소멸했다.");
            }
        }
    }

    private String tierTag(String tier) {
        if (tier == null) return "lower";
        if (tier.contains("드래곤") || tier.contains("용")) return "dragon";
        if (tier.contains("대승") || tier.contains("도겁")) return "higher";
        if (tier.contains("합체") || tier.contains("화신")) return "middle";
        return "lower";
    }

    // ============================================================
    // 깨달음 — 내면 분신 + 3 철학 질문
    // ============================================================
    private static final String[][] QUESTIONS = {
        {"무력은 무엇을 위해 존재하는가?", "보호", "지배", "초월", "무의미"},
        {"적을 마주했을 때, 첫 행동은?", "대화", "선공", "회피", "관찰"},
        {"천하제일이 된 후, 다음은?", "은퇴", "후학 양성", "더 높은 경지", "세계 정복"},
        {"검의 의미는?", "도구", "친구", "자신의 일부", "해방"},
        {"죽음 앞에서, 너의 마지막 말은?", "후회 없다", "살고 싶다", "다시 보자", "침묵"}
    };

    public void startEnlightenment(Player p) {
        if (enlightenment.containsKey(p.getUniqueId())) {
            Msg.warn(p, "이미 깨달음 중이다."); return;
        }
        Bukkit.broadcastMessage("§d§l[깨달음] §f" + p.getName() + "이(가) 내면 세계로 들어선다.");
        p.sendTitle("§d§l깨달음", "§7내면의 분신과 대결하라", 10, 80, 20);

        // 가상 전투 시뮬레이션 (분신 = 플레이어 1.2배)
        double playerStat = RebornCore.get().api().getTotalStats(p.getUniqueId());
        double cloneStat = playerStat * plugin.getConfig().getDouble("minigame.enlightenment.clone-stat-mult", 1.2);

        EnlightenmentSession s = new EnlightenmentSession(playerStat, cloneStat);
        enlightenment.put(p.getUniqueId(), s);

        // 3개 질문 — 30초 간격
        int questionCount = plugin.getConfig().getInt("minigame.enlightenment.questions", 3);
        for (int i = 0; i < questionCount; i++) {
            final int idx = i;
            RebornCore.get().scheduler().runTaskLater(() -> askQuestion(p, s, idx), 600L * idx + 100L);
        }
        // 최종 결과
        RebornCore.get().scheduler().runTaskLater(() -> finishEnlightenment(p, s),
                600L * questionCount + 200L);
    }

    private void askQuestion(Player p, EnlightenmentSession s, int idx) {
        if (!p.isOnline()) return;
        String[] q = QUESTIONS[Rand.range(0, QUESTIONS.length - 1)];
        s.currentQuestion = q;
        s.answeredCurrent = false;
        Msg.send(p, "&d&l[깨달음] " + q[0]);
        for (int i = 1; i < q.length; i++) {
            p.sendMessage("§f" + i + ". §7" + q[i] + " §8(/answer " + i + ")");
        }
    }

    public void onAnswer(Player p, int choice) {
        EnlightenmentSession s = enlightenment.get(p.getUniqueId());
        if (s == null || s.currentQuestion == null || s.answeredCurrent) return;
        s.answeredCurrent = true;
        s.answers.add(choice);
        // "철학적 깊이"로 가산점 (답이 무엇이든 답했다는 사실)
        s.depth += 1.0;
        // 짝수번째 선택지 = 정공법, 홀수 = 변칙
        if (choice % 2 == 0) s.depth += 0.5;
        Msg.send(p, "&7깨달음 깊이 +1");
    }

    private void finishEnlightenment(Player p, EnlightenmentSession s) {
        enlightenment.remove(p.getUniqueId());
        // 분신과의 가상 전투 결과: 플레이어 스탯 + 깨달음 깊이 vs 분신 스탯
        double effective = s.playerStats + s.depth * 100;
        if (effective >= s.cloneStats) {
            Bukkit.broadcastMessage("§6§l[생사경 돌파] §f" + p.getName() + "이(가) 깨달음을 얻었다!");
            p.sendTitle("§6§l깨달음 성공", "§f생사경 돌파", 10, 100, 20);
            RebornCore.get().api().addStat(p.getUniqueId(), StatType.INNER_KI, 500, "enlightenment");
            RebornCore.get().api().addStat(p.getUniqueId(), StatType.MENTAL, 50, "enlightenment");
        } else {
            Msg.error(p, "&c깨달음 실패. 분신을 이기지 못했다.");
            p.sendTitle("§4§l실패", "§7더 수련해야 한다", 10, 60, 20);
            RebornCore.get().api().addStat(p.getUniqueId(), StatType.MENTAL, -10, "enlightenment-fail");
        }
    }

    // ============================================================
    // 외부 API
    // ============================================================
    public boolean isInTribulation(UUID id) { return tribulation.containsKey(id); }
    public boolean isInEnlightenment(UUID id) { return enlightenment.containsKey(id); }

    public void noteHit(Player p) {
        TribulationSession s = tribulation.get(p.getUniqueId());
        if (s != null) s.hits++;
    }

    // ============================================================
    private static final class MeditationSession {
        final List<String> sequence;
        final List<String> inputs = new ArrayList<>();
        final long deadline;
        final int tier;
        MeditationSession(List<String> s, long d, int tier) { sequence = s; deadline = d; this.tier = tier; }
    }

    private static final class TribulationSession {
        final int totalBolts;
        final int simultaneous;
        final double intervalSeconds;
        final String tierName;
        int hits = 0;
        int tolerance = 3;
        TribulationSession(int total, int simul, double iv, String tier) {
            totalBolts = total; simultaneous = simul; intervalSeconds = iv; tierName = tier;
        }
    }

    private static final class EnlightenmentSession {
        final double playerStats;
        final double cloneStats;
        double depth = 0;
        String[] currentQuestion;
        boolean answeredCurrent = true;
        final List<Integer> answers = new ArrayList<>();
        EnlightenmentSession(double p, double c) { playerStats = p; cloneStats = c; }
    }
}
