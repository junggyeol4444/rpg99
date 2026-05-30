package kr.reborn.clan.war;

import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Msg;
import kr.reborn.clan.RebornClan;
import kr.reborn.clan.data.Clan;
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
 * 가문 전쟁 매니저.
 *
 * 선전포고 → ClanWar 인스턴스 + 양 가문 멤버에 PK 면책 + 점수 누적 시작.
 * 7일 자동 종전 or treasury 0 → 굴복.
 * 종전 시:
 *   - 패전 가문 treasury 30% → 승전 가문
 *   - 영토 1개 자동 이양 (TerritoryManager hook)
 *   - 패전 가문 lineage 손상 (xp -10%)
 */
public final class ClanWarManager implements Listener {

    private static final long WAR_MAX_MS = 7 * 24 * 3600_000L;
    private static final double SPOIL_RATIO = 0.30;

    private final RebornClan plugin;
    /** clanId → war (양쪽 모두 등록) */
    private final Map<String, ClanWar> wars = new HashMap<>();

    public ClanWarManager(RebornClan plugin) {
        this.plugin = plugin;
        // 1분마다 종전 체크
        RebornCore.get().scheduler().runTimer(this::tick, 1200L, 1200L);
    }

    public boolean declareWar(Player p, String enemyClanId) {
        Clan my = plugin.clans().ofPlayer(p.getUniqueId());
        if (my == null) { Msg.error(p, "가문 소속이 아니다."); return false; }
        if (!my.leader.equals(p.getUniqueId()) && !my.elders.contains(p.getUniqueId())) {
            Msg.error(p, "가주·원로만 선전포고 가능."); return false;
        }
        if (wars.containsKey(my.id)) { Msg.error(p, "이미 전쟁중."); return false; }
        Clan enemy = plugin.clans().get(enemyClanId);
        if (enemy == null) { Msg.error(p, "가문 없음: " + enemyClanId); return false; }
        if (enemy.id.equals(my.id)) { Msg.error(p, "자신과 전쟁 불가."); return false; }
        if (wars.containsKey(enemy.id)) { Msg.error(p, "상대가 이미 전쟁중."); return false; }

        ClanWar w = new ClanWar(my.id, enemy.id);
        wars.put(my.id, w);
        wars.put(enemy.id, w);
        // 양측 멤버를 PK 면책 set에 추가
        w.immunePlayers.addAll(my.members);
        w.immunePlayers.addAll(enemy.members);

        Bukkit.broadcastMessage("§4§l[가문 전쟁] §f" + my.name + " §c⚔ §f" + enemy.name + " §7— 7일간 전쟁!");
        notifyMembers(my, "§c너의 가문이 " + enemy.name + "과(와) 전쟁에 들어갔다!");
        notifyMembers(enemy, "§c너의 가문이 " + my.name + "과(와) 전쟁에 들어갔다!");
        return true;
    }

    public boolean joinAsAlly(Player p, String challengerClanId) {
        Clan my = plugin.clans().ofPlayer(p.getUniqueId());
        if (my == null) { Msg.error(p, "가문 소속이 아니다."); return false; }
        if (!my.leader.equals(p.getUniqueId())) {
            Msg.error(p, "가주만 동맹 참전 가능."); return false;
        }
        if (wars.containsKey(my.id)) { Msg.error(p, "이미 전쟁중."); return false; }
        ClanWar w = wars.get(challengerClanId);
        if (w == null || !w.active()) { Msg.error(p, "활성 전쟁 없음."); return false; }
        if (w.isChallengerSide(challengerClanId)) {
            w.challengerAllies.add(my.id);
        } else {
            w.defenderAllies.add(my.id);
        }
        wars.put(my.id, w);
        w.immunePlayers.addAll(my.members);
        Bukkit.broadcastMessage("§5§l[동맹 참전] §f" + my.name + " §7가 전쟁에 참여!");
        return true;
    }

    public void tick() {
        long now = System.currentTimeMillis();
        for (ClanWar w : new ArrayList<>(new java.util.HashSet<>(wars.values()))) {
            if (!w.active()) continue;
            // 자동 종전
            if (now - w.startedAt >= WAR_MAX_MS) {
                endWar(w, "시간 만료");
                continue;
            }
            // 한 쪽 굴복 — treasury 0
            Clan ch = plugin.clans().get(w.challengerClanId);
            Clan df = plugin.clans().get(w.defenderClanId);
            if (ch != null && ch.treasury <= 0) endWar(w, "도전측 굴복");
            else if (df != null && df.treasury <= 0) endWar(w, "방어측 굴복");
        }
    }

    private void endWar(ClanWar w, String reason) {
        w.endedAt = System.currentTimeMillis();
        Clan winner = plugin.clans().get(w.winnerSide());
        Clan loser = plugin.clans().get(w.loserSide());
        // 양측 모두 wars 맵에서 제거
        wars.remove(w.challengerClanId);
        wars.remove(w.defenderClanId);
        for (String ally : w.challengerAllies) wars.remove(ally);
        for (String ally : w.defenderAllies) wars.remove(ally);

        if (winner != null && loser != null) {
            // 패전 treasury 30% 이양
            double spoils = loser.treasury * SPOIL_RATIO;
            loser.treasury -= spoils;
            winner.treasury += spoils;
            // 패전 xp -10%
            loser.xp = (long)(loser.xp * 0.9);
            // 영토 이양 (TerritoryManager에 위임)
            try {
                var territories = plugin.territories();
                if (territories != null) {
                    var loserTerritories = territories.getClass()
                            .getMethod("ofClan", String.class).invoke(territories, loser.id);
                    if (loserTerritories instanceof java.util.Collection<?> col && !col.isEmpty()) {
                        Object firstT = col.iterator().next();
                        territories.getClass()
                                .getMethod("transferTo", Object.class, String.class)
                                .invoke(territories, firstT, winner.id);
                    }
                }
            } catch (Throwable ignored) {}
            Bukkit.broadcastMessage("§6§l[가문 전쟁 종전] §f" + winner.name
                    + " §a승리 §7vs §f" + loser.name + " §7— " + reason
                    + " §6treasury +" + (int) spoils);
            notifyMembers(winner, "§a전쟁 승리! 보상: " + (int) spoils + " gold");
            notifyMembers(loser, "§c전쟁 패배. 보상 30% + 영토 1개 이양.");
        }
    }

    /** PvP 데미지를 전쟁 점수로 누적 + 면책 처리. */
    @EventHandler
    public void onPvp(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player attacker)) return;
        if (!(e.getEntity() instanceof Player victim)) return;
        Clan ac = plugin.clans().ofPlayer(attacker.getUniqueId());
        Clan vc = plugin.clans().ofPlayer(victim.getUniqueId());
        if (ac == null || vc == null) return;
        ClanWar w = wars.get(ac.id);
        if (w == null || !w.active()) return;
        if (!w.involves(vc.id)) return;
        if (w.isChallengerSide(ac.id) == w.isChallengerSide(vc.id)) return; // 같은 편

        double dmg = e.getFinalDamage();
        if (w.isChallengerSide(ac.id)) w.challengerScore += dmg;
        else w.defenderScore += dmg;
        // 전시 PK는 범죄 안됨 — RebornDeath의 CrimeManager가 이 면책 정보를 조회
    }

    private void notifyMembers(Clan c, String msg) {
        for (UUID u : c.members) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) p.sendMessage(msg);
        }
    }

    /** 외부에서 전시 면책 조회 (RebornDeath CrimeManager). */
    public boolean isImmuneByWar(UUID p) {
        for (ClanWar w : wars.values()) {
            if (w.active() && w.immunePlayers.contains(p)) return true;
        }
        return false;
    }

    public List<ClanWar> activeWars() {
        return new ArrayList<>(new java.util.HashSet<>(wars.values()));
    }
}
