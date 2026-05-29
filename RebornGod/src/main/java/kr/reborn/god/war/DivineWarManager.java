package kr.reborn.god.war;

import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Msg;
import kr.reborn.god.RebornGod;
import kr.reborn.god.data.God;
import kr.reborn.god.data.Religion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 신 전쟁 매니저 — 신 간 전쟁 선포·점수 집계·종전 처리.
 *
 * 전쟁 중 양측 신도가 서로에게 가한 피해가 전쟁 점수로 누적된다.
 * 7일 자동 종전 또는 한 측 신도가 절멸하면 즉시 종전.
 * 패전 신은 신앙의 30%를 승전 신에게 이양 + 신도 일부 이탈.
 */
public final class DivineWarManager implements Listener {

    /** 전쟁 자동 종전 — 7일. */
    private static final long WAR_MAX_MS = 7 * 24 * 3600_000L;
    /** 패전 신 → 승전 신 신앙 이양 비율. */
    private static final double SPOIL_RATIO = 0.30;

    private final RebornGod plugin;
    /** 활성 전쟁 — godId → war. (양 신 모두 등록) */
    private final Map<String, DivineWar> wars = new HashMap<>();

    public DivineWarManager(RebornGod plugin) {
        this.plugin = plugin;
        // 1분마다 종전 체크
        RebornCore.get().scheduler().runTimer(this::tick, 1200L, 1200L);
    }

    public boolean declareWar(Player p, String enemyGodId) {
        God my = plugin.gods().of(p.getUniqueId());
        if (my == null) { Msg.error(p, "신만이 선전포고 할 수 있다."); return false; }
        if (my.atWar()) { Msg.error(p, "이미 다른 신과 전쟁중."); return false; }
        // 자기 자신 검증
        String myId = my.identifier();
        if (myId.equals(enemyGodId)) { Msg.error(p, "자기 자신에게 선전포고 불가."); return false; }
        // 대상 신 검증
        God enemy = lookup(enemyGodId);
        if (enemy == null) { Msg.error(p, "대상 신 없음: " + enemyGodId); return false; }
        if (enemy.atWar()) { Msg.error(p, enemy.name + "은(는) 이미 다른 신과 전쟁중."); return false; }
        if (enemy.sealed) { Msg.error(p, "봉인된 신에게 선전포고 불가."); return false; }

        DivineWar w = new DivineWar(myId, enemyGodId);
        wars.put(myId, w);
        wars.put(enemyGodId, w);
        my.warOpponent = enemyGodId; my.warStartedAt = w.startedAt;
        enemy.warOpponent = myId;     enemy.warStartedAt = w.startedAt;

        Bukkit.broadcastMessage("§4§l[신 전쟁] §f" + my.name + " §c⚔ §f" + enemy.name + " §7— 신앙 다툼 시작!");
        // 적신 신도에게 통지
        notifyFollowers(enemy, "§c너의 신이 " + my.name + "과(와) 전쟁에 들어갔다.");
        notifyFollowers(my, "§c너의 신이 " + enemy.name + "과(와) 전쟁에 들어갔다.");
        return true;
    }

    /** 매 1분 — 종전 체크. */
    public void tick() {
        long now = System.currentTimeMillis();
        for (var it = new ArrayList<>(wars.values()).iterator(); it.hasNext();) {
            DivineWar w = it.next();
            if (!w.active()) continue;
            // 자동 종전 (시간 만료)
            if (now - w.startedAt >= WAR_MAX_MS) endWar(w, "시간 만료");
            else {
                // 신도 절멸 체크
                int chFol = countFollowers(w.challengerGodId);
                int dfFol = countFollowers(w.defenderGodId);
                if (chFol == 0) endWar(w, "도전측 신도 절멸");
                else if (dfFol == 0) endWar(w, "방어측 신도 절멸");
            }
        }
    }

    private void endWar(DivineWar w, String reason) {
        w.endedAt = System.currentTimeMillis();
        God winner = lookup(w.winnerSide());
        God loser = lookup(w.loserSide());
        if (winner != null) winner.warOpponent = "";
        if (loser != null) loser.warOpponent = "";
        wars.remove(w.challengerGodId);
        wars.remove(w.defenderGodId);

        // 패전 신앙·신성 이양
        if (winner != null && loser != null) {
            double spoils = loser.divinity * SPOIL_RATIO;
            loser.divinity -= spoils;
            winner.divinity += spoils;
            // 패전 교단의 신앙도 일부 흡수
            for (Religion r : plugin.religions().all()) {
                if (loser.identifier().equals(r.godIdentifier) || (loser.npcId.equals(r.godIdentifier))) {
                    double f = r.faith * 0.20;
                    r.faith -= f;
                    // 승전 신 교단으로 이양
                    for (Religion wr : plugin.religions().all()) {
                        if (winner.identifier().equals(wr.godIdentifier) || winner.npcId.equals(wr.godIdentifier)) {
                            wr.faith += f;
                            break;
                        }
                    }
                }
            }
        }
        Bukkit.broadcastMessage("§6§l[신 전쟁 종전] §f" + (winner != null ? winner.name : "?")
                + " §7vs §f" + (loser != null ? loser.name : "?")
                + " §7— " + reason + ". §a승: " + (winner != null ? winner.name : "?"));
    }

    /** 신도 간 PvP 피해 — 전쟁 점수 누적. */
    @EventHandler
    public void onPvp(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player attacker)) return;
        if (!(e.getEntity() instanceof Player victim)) return;
        God ag = followedGod(attacker);
        God vg = followedGod(victim);
        if (ag == null || vg == null) return;
        if (!ag.atWar() || !vg.atWar()) return;
        if (!ag.warOpponent.equals(vg.identifier()) && !ag.warOpponent.equals(vg.npcId)) return;
        DivineWar w = wars.get(ag.identifier());
        if (w == null) return;
        double dmg = e.getFinalDamage();
        if (w.challengerGodId.equals(ag.identifier())) w.challengerScore += dmg;
        else w.defenderScore += dmg;
    }

    /** 한 플레이어가 섬기는 신 (faith 흐름 기준). */
    private God followedGod(Player p) {
        for (Religion r : plugin.religions().all()) {
            if (r.followers.contains(p.getUniqueId())) {
                return lookup(r.godIdentifier);
            }
        }
        return null;
    }

    private int countFollowers(String godId) {
        God g = lookup(godId);
        if (g == null) return 0;
        int sum = g.followers.size();
        for (Religion r : plugin.religions().all()) {
            if (godId.equals(r.godIdentifier) || (g.npcId.equals(r.godIdentifier))) {
                sum += r.followers.size();
            }
        }
        return sum;
    }

    private God lookup(String id) {
        if (id == null || id.isEmpty()) return null;
        if (id.startsWith("player:")) {
            try { return plugin.gods().of(UUID.fromString(id.substring(7))); }
            catch (Exception e) { return null; }
        }
        String npcId = id.startsWith("npc:") ? id.substring(4) : id;
        for (God g : plugin.gods().npcAll()) if (npcId.equals(g.npcId)) return g;
        return null;
    }

    private void notifyFollowers(God g, String msg) {
        for (UUID u : g.followers) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) p.sendMessage(msg);
        }
        for (Religion r : plugin.religions().all()) {
            if (g.identifier().equals(r.godIdentifier) || g.npcId.equals(r.godIdentifier)) {
                for (UUID u : r.followers) {
                    Player p = Bukkit.getPlayer(u);
                    if (p != null) p.sendMessage(msg);
                }
            }
        }
    }

    public List<DivineWar> activeWars() {
        return new ArrayList<>(new java.util.HashSet<>(wars.values()));
    }
}
