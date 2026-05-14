package kr.reborn.stat.minigame;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.StatType;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import kr.reborn.stat.RebornStat;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 운기조식·천겁·깨달음 미니게임을 호스팅한다.
 * 키 입력 추적은 채팅으로 단순화 (실제 키보드 후킹은 클라이언트 모드 필요).
 */
public final class MinigameManager {

    private final RebornStat plugin;
    private final ConcurrentHashMap<UUID, MeditationSession> meditation = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, TribulationSession> tribulation = new ConcurrentHashMap<>();

    public MinigameManager(RebornStat plugin) { this.plugin = plugin; }

    // === 운기조식 ===
    public void startMeditation(Player p, int tier) {
        var c = plugin.getConfig();
        int keys = Math.min(c.getInt("minigame.meditation.max-keys", 8),
                c.getInt("minigame.meditation.base-keys", 4) + tier);
        double seconds = Math.max(c.getInt("minigame.meditation.min-time", 3),
                c.getInt("minigame.meditation.base-time", 5) - tier * 0.5);
        List<String> seq = new ArrayList<>();
        String pool = "wasdqe";
        for (int i = 0; i < keys; i++) seq.add(String.valueOf(pool.charAt(Rand.range(0, pool.length() - 1))));
        meditation.put(p.getUniqueId(), new MeditationSession(seq, System.currentTimeMillis() + (long)(seconds * 1000)));
        Msg.send(p, "&e운기조식 시작! 채팅에 다음 키를 정확히 입력: &f" + String.join(" ", seq));
    }

    public void onMeditationInput(Player p, String input) {
        MeditationSession s = meditation.remove(p.getUniqueId());
        if (s == null) return;
        if (System.currentTimeMillis() > s.deadline) {
            Msg.error(p, "시간 초과 — 주화입마 위험!");
            // 작은 확률로 주화입마 (RebornCurse 연동시 적용)
            if (Rand.chance(0.15)) Msg.error(p, "주화입마! 정신력 -5");
            return;
        }
        String expected = String.join("", s.sequence).replaceAll(" ", "");
        String got = input.replaceAll("\\s+", "");
        if (got.equalsIgnoreCase(expected)) {
            Msg.send(p, "&a완벽 성공! 내공 +" + 5);
            plugin.growth().meditate(p, 1.0);
        } else {
            int matches = 0;
            for (int i = 0; i < Math.min(got.length(), expected.length()); i++) {
                if (Character.toLowerCase(got.charAt(i)) == Character.toLowerCase(expected.charAt(i))) matches++;
            }
            double q = (double) matches / expected.length();
            Msg.warn(p, "부분 성공 (" + (int)(q*100) + "%) 내공 +" + (int)(5 * q));
            plugin.growth().meditate(p, q);
        }
    }

    // === 천겁 ===
    public void startTribulation(Player p, String tierName) {
        int bolts = plugin.getConfig().getInt("minigame.tribulation.bolts-by-tier." +
                tierTag(tierName), 10);
        TribulationSession s = new TribulationSession(bolts, 0);
        tribulation.put(p.getUniqueId(), s);
        Msg.send(p, "&5천겁 시작! &f낙뢰 " + bolts + "발 회피하라.");
        scheduleBolts(p, s, bolts);
    }

    private void scheduleBolts(Player p, TribulationSession s, int bolts) {
        for (int i = 0; i < bolts; i++) {
            final int idx = i;
            RebornCore.get().scheduler().runTaskLater(() -> {
                if (!p.isOnline()) return;
                p.getWorld().strikeLightning(p.getLocation().add(Rand.rangeD(-3, 3), 0, Rand.rangeD(-3, 3)));
                if (idx == bolts - 1) finishTribulation(p, s);
            }, (long)(20L * (1 + idx * 0.3)));
        }
    }

    private void finishTribulation(Player p, TribulationSession s) {
        tribulation.remove(p.getUniqueId());
        int tolerance = plugin.getConfig().getInt("minigame.tribulation.success-tolerance", 3);
        if (s.hits <= tolerance) {
            Msg.send(p, "&6천겁 통과! 경지 돌파.");
            // 경지는 호출 측에서 추가 진행
        } else {
            Msg.error(p, "천겁 실패 — 스탯 손실");
            RebornCore.get().api().addStat(p.getUniqueId(), StatType.MENTAL, -10, "tribulation-fail");
        }
    }

    private String tierTag(String tier) {
        if (tier.contains("드래곤") || tier.contains("용")) return "dragon";
        if (tier.contains("대승") || tier.contains("도겁")) return "higher";
        if (tier.contains("합체") || tier.contains("화신")) return "middle";
        return "lower";
    }

    public void noteHit(Player p) {
        TribulationSession s = tribulation.get(p.getUniqueId());
        if (s != null) s.hits++;
    }

    // === 깨달음 ===
    public void startEnlightenment(Player p) {
        Msg.send(p, "&d깨달음 — 내면의 분신과 대결하라.");
        // 간이 구현: 5초 후 50% 확률 성공
        RebornCore.get().scheduler().runTaskLater(() -> {
            if (Rand.chance(0.5)) {
                Msg.send(p, "&6깨달음 성공! 생사경 돌파.");
            } else {
                Msg.error(p, "깨달음 실패. 다시 도전해라.");
                RebornCore.get().api().addStat(p.getUniqueId(), StatType.MENTAL, -5, "enlightenment-fail");
            }
        }, 100L);
    }

    private static final class MeditationSession {
        final List<String> sequence; final long deadline;
        MeditationSession(List<String> s, long d) { sequence = s; deadline = d; }
    }
    private static final class TribulationSession {
        int total; int hits;
        TribulationSession(int total, int hits) { this.total = total; this.hits = hits; }
    }
}
