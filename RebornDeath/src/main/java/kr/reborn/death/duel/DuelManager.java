package kr.reborn.death.duel;

import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Msg;
import kr.reborn.death.RebornDeath;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 결투 매니저 — 1v1 명예 시스템.
 *
 * 흐름:
 *   1. /duel challenge <player> — 도전장
 *   2. 상대가 /duel accept 30초 이내 수락
 *   3. 양쪽 풀체력 + 격리 (다른 데미지 면역)
 *   4. 한쪽 HP 1 도달 → 자동 종료, 패자는 명예 -10, 승자 +10
 *   5. 사망 시 부활 (실제 사망 안 됨, 명계 안 보냄)
 *
 * 명예 효과:
 *   - 명예 100+ = 결투 거부 안 됨 (명예의 챔피언)
 *   - 명예 -50 이하 = 결투 신청 불가 (불명예)
 *
 * 결투 중 PvP는 범죄 안 됨 (Crime 면책).
 */
public final class DuelManager {

    public enum State { CHALLENGED, ACTIVE, ENDED }

    public static final class Duel {
        public final UUID challenger;
        public final UUID defender;
        public final long createdAt;
        public State state;
        public UUID winnerId;
        public Location savedPosA;
        public Location savedPosB;

        public Duel(UUID a, UUID b) {
            this.challenger = a; this.defender = b;
            this.createdAt = System.currentTimeMillis();
            this.state = State.CHALLENGED;
        }
    }

    private final RebornDeath plugin;
    /** 양쪽 모두 매핑 */
    private final Map<UUID, Duel> duels = new ConcurrentHashMap<>();
    /** uuid → 명예 점수 */
    private final Map<UUID, Integer> honor = new ConcurrentHashMap<>();

    public DuelManager(RebornDeath plugin) {
        this.plugin = plugin;
        // 30초마다 만료 도전 정리
        RebornCore.get().scheduler().runTimer(this::tickExpire, 200L, 200L);
    }

    public boolean challenge(Player challenger, Player defender) {
        if (challenger == defender) {
            Msg.error(challenger, "자기 자신에게 결투 불가."); return false;
        }
        if (duels.containsKey(challenger.getUniqueId())) {
            Msg.error(challenger, "이미 결투 중."); return false;
        }
        if (duels.containsKey(defender.getUniqueId())) {
            Msg.error(challenger, "상대가 이미 결투 중."); return false;
        }
        // honorOf — KV lazy 로드 포함 (캐시만 보면 저장된 불명예를 우회 가능했음)
        int myHonor = honorOf(challenger.getUniqueId());
        if (myHonor < -50) {
            Msg.error(challenger, "불명예 (명예 " + myHonor + ") — 결투 신청 불가.");
            return false;
        }
        Duel d = new Duel(challenger.getUniqueId(), defender.getUniqueId());
        duels.put(challenger.getUniqueId(), d);
        duels.put(defender.getUniqueId(), d);
        Msg.send(challenger, "&6도전장 발송 — 30초 안에 상대가 수락하지 않으면 자동 취소.");
        Msg.send(defender, "&6&l[결투 신청] &f" + challenger.getName()
                + " §7가 1v1 결투 신청 — /duel accept 또는 /duel reject");
        return true;
    }

    public boolean accept(Player defender) {
        Duel d = duels.get(defender.getUniqueId());
        if (d == null || d.state != State.CHALLENGED || !defender.getUniqueId().equals(d.defender)) {
            Msg.error(defender, "수락할 결투 없음."); return false;
        }
        d.state = State.ACTIVE;
        Player ch = Bukkit.getPlayer(d.challenger);
        if (ch == null) { cancel(d, "도전자 오프라인"); return false; }
        d.savedPosA = ch.getLocation();
        d.savedPosB = defender.getLocation();
        // 양쪽 풀체력 + 효과
        try {
            ch.setHealth(ch.getMaxHealth());
            defender.setHealth(defender.getMaxHealth());
            ch.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 12000, 0));
            defender.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 12000, 0));
        } catch (Throwable ignored) {}
        Bukkit.broadcastMessage("§6§l[결투 시작] §f" + ch.getName() + " §6vs §f" + defender.getName());
        return true;
    }

    public boolean reject(Player defender) {
        Duel d = duels.get(defender.getUniqueId());
        if (d == null || d.state != State.CHALLENGED) { Msg.warn(defender, "거절할 결투 없음."); return false; }
        cancel(d, "거절");
        return true;
    }

    /** 데미지로 인한 패배 확인 — DeathListener에서 호출 가능. */
    public boolean onDamage(Player victim) {
        Duel d = duels.get(victim.getUniqueId());
        if (d == null || d.state != State.ACTIVE) return false;
        if (victim.getHealth() <= 1) {
            // 패배
            UUID winnerId = victim.getUniqueId().equals(d.challenger) ? d.defender : d.challenger;
            d.winnerId = winnerId;
            endDuel(d, victim.getUniqueId());
            return true;
        }
        return false;
    }

    /** 결투 중 PvP 면책. */
    public boolean isInActiveDuel(UUID p) {
        Duel d = duels.get(p);
        return d != null && d.state == State.ACTIVE;
    }

    /** 결투 상대인지 확인. */
    public boolean areOpponents(UUID a, UUID b) {
        Duel d = duels.get(a);
        if (d == null || d.state != State.ACTIVE) return false;
        return (d.challenger.equals(a) && d.defender.equals(b))
                || (d.challenger.equals(b) && d.defender.equals(a));
    }

    private void endDuel(Duel d, UUID loserId) {
        d.state = State.ENDED;
        UUID winnerId = d.winnerId;
        Player winner = Bukkit.getPlayer(winnerId);
        Player loser = Bukkit.getPlayer(loserId);
        duels.remove(d.challenger);
        duels.remove(d.defender);
        if (winner != null) {
            try { winner.setHealth(winner.getMaxHealth()); } catch (Throwable ignored) {}
            int newH = honor.merge(winnerId, 10, Integer::sum);
            try { kr.reborn.core.RebornCore.get().kv()
                    .putInt("RebornDeath.duel", winnerId, "honor", newH); }
            catch (Throwable ignored) {}
        }
        if (loser != null) {
            try { loser.setHealth(loser.getMaxHealth()); } catch (Throwable ignored) {}
            int newH = honor.merge(loserId, -10, Integer::sum);
            try { kr.reborn.core.RebornCore.get().kv()
                    .putInt("RebornDeath.duel", loserId, "honor", newH); }
            catch (Throwable ignored) {}
            // 원위치로 복귀
            if (d.savedPosA != null && loserId.equals(d.challenger)) loser.teleport(d.savedPosA);
            else if (d.savedPosB != null) loser.teleport(d.savedPosB);
        }
        Bukkit.broadcastMessage("§6§l[결투 종료] §f"
                + (winner != null ? winner.getName() : "?") + " §a승리 §7vs §f"
                + (loser != null ? loser.getName() : "?") + " §c패배");
        if (winner != null) Msg.send(winner, "&6명예 +10 (현재 " + honor.getOrDefault(winnerId, 0) + ")");
        if (loser != null) Msg.send(loser, "&c명예 -10 (현재 " + honor.getOrDefault(loserId, 0) + ")");
    }

    private void cancel(Duel d, String reason) {
        duels.remove(d.challenger);
        duels.remove(d.defender);
        Player ch = Bukkit.getPlayer(d.challenger);
        if (ch != null) Msg.send(ch, "&7결투 취소: " + reason);
        Player df = Bukkit.getPlayer(d.defender);
        if (df != null) Msg.send(df, "&7결투 취소: " + reason);
    }

    private void tickExpire() {
        long now = System.currentTimeMillis();
        for (var entry : new HashMap<>(duels).entrySet()) {
            Duel d = entry.getValue();
            if (d.state == State.CHALLENGED && now - d.createdAt > 30_000L) {
                cancel(d, "시간 만료");
            } else if (d.state == State.ACTIVE) {
                // 결투 중 로그아웃 — 탈주자 패배 처리 (이전엔 duels 엔트리가
                // 영구 잔류해 양쪽 다 재시작까지 '이미 결투 중' 잠금)
                boolean chOffline = Bukkit.getPlayer(d.challenger) == null;
                boolean dfOffline = Bukkit.getPlayer(d.defender) == null;
                if (chOffline && dfOffline) {
                    duels.remove(d.challenger);
                    duels.remove(d.defender);
                } else if (chOffline) {
                    d.winnerId = d.defender;
                    endDuel(d, d.challenger);
                    Bukkit.broadcastMessage("§7[결투] 도전자 탈주 — 부전승.");
                } else if (dfOffline) {
                    d.winnerId = d.challenger;
                    endDuel(d, d.defender);
                    Bukkit.broadcastMessage("§7[결투] 응전자 탈주 — 부전승.");
                }
            }
        }
    }

    public int honorOf(UUID p) {
        Integer cached = honor.get(p);
        if (cached != null) return cached;
        // DB에서 로드 (없으면 50 기본)
        int h = kr.reborn.core.RebornCore.get().kv()
                .getInt("RebornDeath.duel", p, "honor", 50);
        honor.put(p, h);
        return h;
    }

    public Map<UUID, Integer> honorAll() { return honor; }
}
