package kr.reborn.god.trial;

import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import kr.reborn.god.RebornGod;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 시련 관리자 — 진행중인 시련 추적, 단계별 성공/실패 판정, 등극 처리.
 *
 * POWER: 강한 적(HP 1000+) 3체 격파 시 통과.
 * WISDOM: 3개 수수께끼 정답. 한 번 틀리면 시련 중단(재시도 가능).
 * CONVICTION: 5분간 위치 유지(위험 환경 견디기). 사망 시 실패.
 */
public final class TrialManager implements Listener {

    /** POWER 시련 — 적 최소 HP 임계치. */
    private static final double POWER_MIN_HP = 1000;
    /** 통과에 필요한 적 격파 수. */
    private static final int POWER_KILLS_REQUIRED = 3;
    /** WISDOM — 통과에 필요한 정답 수. */
    private static final int WISDOM_REQUIRED = 3;
    /** CONVICTION — 견뎌야 하는 시간(ms). */
    private static final long CONVICTION_DURATION = 300_000;  // 5분
    /** 등극 요구 총합 스탯. */
    private static final int ASCEND_TOTAL_STAT = 5000;

    private final RebornGod plugin;
    private final Map<UUID, AscensionTrial> active = new HashMap<>();
    /** 수수께끼 (질문/정답 배열). 미정해진 정답엔 lowercase 매칭. */
    private final List<String[]> riddles = List.of(
            new String[]{ "&7태초에 있었으나 끝에는 없는 것은?", "시간" },
            new String[]{ "&7많을수록 가벼워지는 것은?", "구멍" },
            new String[]{ "&7주면 점점 커지는 것은?", "구덩이" },
            new String[]{ "&7항상 오지만 절대 도착하지 않는 것은?", "내일" },
            new String[]{ "&7불에 던져도 타지 않고 물에 빠져도 젖지 않는 것은?", "그림자" },
            new String[]{ "&7만들 수는 있어도 볼 수는 없는 것은?", "소리" },
            new String[]{ "&7가질수록 잃는 것은?", "비밀" },
            new String[]{ "&7많이 가지면 가난해지는 것은?", "거짓말" },
            new String[]{ "&7주인을 따르되 주인이 없는 것은?", "그림자" },
            new String[]{ "&7한 번 깨지면 절대 고칠 수 없는 것은?", "약속" }
    );

    public TrialManager(RebornGod plugin) {
        this.plugin = plugin;
        // 의지 시련 1초마다 체크
        RebornCore.get().scheduler().runTimer(this::tickConviction, 20L, 20L);
    }

    public boolean startTrial(Player p) {
        if (active.containsKey(p.getUniqueId())) {
            Msg.warn(p, "이미 시련 진행중. /god trial status로 확인.");
            return false;
        }
        if (plugin.gods().of(p.getUniqueId()) != null) {
            Msg.warn(p, "이미 신이다."); return false;
        }
        double total = RebornCore.get().api().getTotalStats(p.getUniqueId());
        if (total < ASCEND_TOTAL_STAT) {
            Msg.error(p, "절대자(총합 " + ASCEND_TOTAL_STAT + "+)가 되어야 시련을 시작할 수 있다. 현재 " + (int) total);
            return false;
        }
        active.put(p.getUniqueId(), new AscensionTrial(p.getUniqueId()));
        Msg.send(p, "&6&l[신 등극 시련 시작]");
        Msg.send(p, "&7단계 1 — &c&l권능 시련&r&7: HP " + (int) POWER_MIN_HP
                + "+ 적 " + POWER_KILLS_REQUIRED + "체 격파");
        Msg.send(p, "&7단계 2 — &b&l지혜 시련&r&7: 수수께끼 " + WISDOM_REQUIRED + "개 정답 (/god riddle)");
        Msg.send(p, "&7단계 3 — &5&l의지 시련&r&7: 위험 환경 5분 생존 (/god trial conviction)");
        return true;
    }

    public void status(Player p) {
        AscensionTrial t = active.get(p.getUniqueId());
        if (t == null) { Msg.send(p, "&7시련 미진행."); return; }
        Msg.send(p, "&6=== 신 등극 시련 진행 상황 ===");
        Msg.send(p, " &c권능: " + check(t, AscensionTrial.Stage.POWER) + " " + t.powerKills + "/" + POWER_KILLS_REQUIRED);
        Msg.send(p, " &b지혜: " + check(t, AscensionTrial.Stage.WISDOM) + " " + t.wisdomCorrect + "/" + WISDOM_REQUIRED);
        Msg.send(p, " &5의지: " + check(t, AscensionTrial.Stage.CONVICTION));
        Msg.send(p, "&7다음 단계: &e" + t.nextStage());
    }

    private String check(AscensionTrial t, AscensionTrial.Stage s) {
        return t.passed.contains(s) ? "&a&l✔" : "&7☐";
    }

    /** WISDOM — 다음 수수께끼 출제. */
    public void askRiddle(Player p) {
        AscensionTrial t = active.get(p.getUniqueId());
        if (t == null) { Msg.error(p, "시련 미시작. /god trial start"); return; }
        if (t.passed.contains(AscensionTrial.Stage.WISDOM)) {
            Msg.warn(p, "지혜 시련 이미 통과."); return;
        }
        // 미출제 수수께끼 중에서 무작위
        int idx = -1;
        for (int tries = 0; tries < 20; tries++) {
            int r = Rand.range(0, riddles.size() - 1);
            if (!t.askedRiddles.contains(r)) { idx = r; break; }
        }
        if (idx < 0) {
            Msg.error(p, "더 이상 출제할 수수께끼가 없다. 시련 실패.");
            active.remove(p.getUniqueId()); return;
        }
        t.currentRiddleIdx = idx;
        t.askedRiddles.add(idx);
        Msg.send(p, "&b&l[수수께끼] &r" + riddles.get(idx)[0]);
        Msg.send(p, "&7정답: /god answer <단어>");
    }

    /** WISDOM — 답변 처리. */
    public void answerRiddle(Player p, String answer) {
        AscensionTrial t = active.get(p.getUniqueId());
        if (t == null || t.currentRiddleIdx < 0) {
            Msg.error(p, "현재 출제된 수수께끼가 없다. /god riddle"); return;
        }
        String correct = riddles.get(t.currentRiddleIdx)[1];
        if (correct.equalsIgnoreCase(answer.trim())) {
            t.wisdomCorrect++;
            t.currentRiddleIdx = -1;
            Msg.send(p, "&a정답! (" + t.wisdomCorrect + "/" + WISDOM_REQUIRED + ")");
            if (t.wisdomCorrect >= WISDOM_REQUIRED) {
                t.passed.add(AscensionTrial.Stage.WISDOM);
                Msg.send(p, "&b&l[지혜 시련 통과]");
                checkAscend(p, t);
            }
        } else {
            Msg.error(p, "오답. 시련을 다시 시작해야 한다.");
            active.remove(p.getUniqueId());
        }
    }

    /** CONVICTION — 시작. */
    public void startConviction(Player p) {
        AscensionTrial t = active.get(p.getUniqueId());
        if (t == null) { Msg.error(p, "시련 미시작."); return; }
        if (t.passed.contains(AscensionTrial.Stage.CONVICTION)) {
            Msg.warn(p, "의지 시련 이미 통과."); return;
        }
        t.convictionStartedAt = System.currentTimeMillis();
        Msg.send(p, "&5&l[의지 시련 시작] §f5분간 견뎌라.");
        // 위협: 위더+슬로우+계속 데미지 환경 부여
        p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 6000, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 6000, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 6000, 1));
    }

    /** 1초마다 — CONVICTION 진행 체크. */
    private void tickConviction() {
        long now = System.currentTimeMillis();
        for (var it = active.entrySet().iterator(); it.hasNext();) {
            var en = it.next();
            AscensionTrial t = en.getValue();
            if (t.passed.contains(AscensionTrial.Stage.CONVICTION)) continue;
            if (t.convictionStartedAt == 0) continue;
            Player p = Bukkit.getPlayer(en.getKey());
            if (p == null || p.isDead()) {
                if (p != null) Msg.error(p, "&c의지 시련 실패 — 사망.");
                t.convictionStartedAt = 0;
                continue;
            }
            long elapsed = now - t.convictionStartedAt;
            if (elapsed >= CONVICTION_DURATION) {
                t.passed.add(AscensionTrial.Stage.CONVICTION);
                Msg.send(p, "&5&l[의지 시련 통과] §f너의 의지는 굳건하다.");
                checkAscend(p, t);
            } else if (elapsed % 60_000 < 1100) {
                Msg.send(p, "&7의지 시련 진행: " + (elapsed / 1000) + "/" + (CONVICTION_DURATION / 1000) + "초");
            }
        }
    }

    /** 적 사망 이벤트 — POWER 진행. */
    @EventHandler
    public void onKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        AscensionTrial t = active.get(killer.getUniqueId());
        if (t == null || t.passed.contains(AscensionTrial.Stage.POWER)) return;
        LivingEntity le = e.getEntity();
        double maxHp = le.getMaxHealth();
        if (maxHp < POWER_MIN_HP) return;
        t.powerKills++;
        Msg.send(killer, "&c권능 시련 진행: " + t.powerKills + "/" + POWER_KILLS_REQUIRED);
        if (t.powerKills >= POWER_KILLS_REQUIRED) {
            t.passed.add(AscensionTrial.Stage.POWER);
            Msg.send(killer, "&c&l[권능 시련 통과]");
            checkAscend(killer, t);
        }
    }

    private void checkAscend(Player p, AscensionTrial t) {
        if (t.passedAll()) {
            active.remove(p.getUniqueId());
            Bukkit.broadcastMessage("§6§l[신 등극] §f" + p.getName()
                    + "이(가) 3 시련을 모두 통과하고 신의 자리에 올랐다!");
            plugin.gods().ascend(p);  // 정상 ascend (시련 통과 표시)
        }
    }

    public AscensionTrial of(UUID id) { return active.get(id); }
}
