package kr.reborn.npc.faction;

import kr.reborn.core.util.Rand;
import kr.reborn.npc.RebornNPC;
import kr.reborn.npc.entity.RebornNpc;
import kr.reborn.npc.social.RelationshipType;
import kr.reborn.npc.soul.Goal;
import kr.reborn.npc.soul.GoalGenerator;
import kr.reborn.npc.soul.GoalKind;
import kr.reborn.npc.soul.GoalProgressor;
import kr.reborn.npc.soul.Personality;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * 세력 형성·외교·전쟁·붕괴를 관리하는 중앙 엔진. NpcRegistry에 1개 인스턴스.
 *
 * 매 주기 tick()에서:
 *   1) adoptPresetFactions — config에서 온 faction 문자열을 살아있는 세력 객체로 승격
 *   2) updateLifecycle     — 죽은 구성원 제거·지도자 승계·와해
 *   3) tryFormNewFactions  — 야망 높은 무소속 NPC가 인맥 모아 세력 창설
 *   4) recruit             — 기존 세력이 근처 우호 NPC 영입
 *   5) updateDiplomacy     — 이념 유사 → 동맹 / 구성원 간 원한 → 전쟁
 *   6) resolveWars         — 전력 차이로 정복·흡수, 소모전은 휴전
 *
 * 관계망·소문·성격을 재사용한다 (새 감정 시스템을 만들지 않음).
 */
public final class FactionManager {

    private static final int MAX_FOUNDING_CORE = 5;  // 창설 시 즉시 합류 최대 측근 수
    private static final int FORM_AMBITION = 55;      // 창설 최소 야망
    private static final int WAR_HOSTILITY = 3;       // 전쟁 발발 최소 구성원 간 원한 수

    private final RebornNPC plugin;
    private final Map<String, Faction> factions = new HashMap<>();

    public FactionManager(RebornNPC plugin) { this.plugin = plugin; }

    public Faction get(String id) { return factions.get(id); }
    public Collection<Faction> all() { return factions.values(); }

    public Faction factionOf(String npcId) {
        RebornNpc n = plugin.registry().get(npcId);
        if (n == null || n.faction.isEmpty()) return null;
        return factions.get(n.faction);
    }

    /** 매 주기 호출 (NpcRegistry에서 N사이클마다). */
    public void tick() {
        adoptPresetFactions();
        updateLifecycle();
        tryFormNewFactions();
        for (Faction f : new ArrayList<>(factions.values())) recruit(f);
        updateDiplomacy();
        resolveWars();
    }

    // ───────────────────────── 창설 ─────────────────────────

    public Faction createFaction(RebornNpc leader, String name) {
        if (leader == null || leader.soul == null) return null;
        String id = "fac_" + leader.id;
        if (factions.containsKey(id)) return factions.get(id);
        Faction f = new Faction(id, name, leader.id);
        f.territoryCenter = leader.location;
        leader.faction = id;
        // 핵심 측근 영입 — 관계망의 긍정 관계(친구·동맹·가족·제자)
        var net = plugin.registry().socialNetwork();
        for (var e : net.neighbors(leader.id).entrySet()) {
            if (f.size() >= MAX_FOUNDING_CORE) break;
            RebornNpc cand = plugin.registry().get(e.getKey());
            if (cand == null || cand.dead || !cand.faction.isEmpty()) continue;
            RelationshipType rt = e.getValue();
            if (rt == RelationshipType.FRIEND || rt == RelationshipType.ALLY
                    || rt == RelationshipType.KIN || rt == RelationshipType.SPOUSE
                    || rt == RelationshipType.STUDENT) {
                joinFaction(cand, f);
            }
        }
        f.recomputeIdeology(plugin.registry()::get);
        factions.put(id, f);
        // FOUND_TOWN / GAIN_POWER 목표 진행 (CLAN_FOUNDED 이벤트)
        plugin.registry().goalProgressor().onEvent(leader,
                new GoalProgressor.Event(GoalProgressor.EventKind.CLAN_FOUNDED, id));
        Bukkit.broadcastMessage("§e§l[세력 창설] §f" + clean(leader.displayName)
                + "이(가) §6" + name + "§f을(를) 세웠다! (구성원 " + f.size() + "명)");
        return f;
    }

    private void joinFaction(RebornNpc npc, Faction f) {
        f.members.add(npc.id);
        npc.faction = f.id;
    }

    private void tryFormNewFactions() {
        var net = plugin.registry().socialNetwork();
        for (RebornNpc npc : plugin.registry().all()) {
            if (npc.dead || npc.soul == null || !npc.faction.isEmpty()) continue;
            if (npc.soul.personality.get(Personality.Trait.AMBITION) < FORM_AMBITION) continue;
            // 무소속 우호 인맥 수
            int allies = 0;
            for (var e : net.neighbors(npc.id).entrySet()) {
                if (e.getValue() == null || !e.getValue().positive) continue;
                RebornNpc cand = plugin.registry().get(e.getKey());
                if (cand != null && !cand.dead && cand.faction.isEmpty()) allies++;
            }
            if (allies < 2) continue;
            if (!Rand.chance(0.15)) continue;  // 점진적 형성
            createFaction(npc, generateName(npc));
        }
    }

    private void recruit(Faction f) {
        RebornNpc leader = plugin.registry().get(f.leaderId);
        if (leader == null || leader.location == null) return;
        for (RebornNpc npc : plugin.registry().all()) {
            if (npc.dead || npc.soul == null || !npc.faction.isEmpty()) continue;
            if (npc.location == null || npc.location.getWorld() != leader.location.getWorld()) continue;
            if (f.territoryCenter != null
                    && npc.location.getWorld() == f.territoryCenter.getWorld()
                    && npc.location.distance(f.territoryCenter) > f.territoryRadius * 2) continue;
            // 지도자에 대한 호감 + 충성 성향이 있어야 합류
            if (npc.soul.relationToward(f.leaderId) < 30) continue;
            if (npc.soul.personality.get(Personality.Trait.LOYALTY) < 0) continue;
            if (!Rand.chance(0.1)) continue;
            joinFaction(npc, f);
            f.recomputeIdeology(plugin.registry()::get);
        }
    }

    // ───────────────────────── 외교·전쟁 ─────────────────────────

    public void setStance(Faction a, Faction b, FactionStance stance) {
        a.relations.put(b.id, stance);
        b.relations.put(a.id, stance.inverse());
    }

    private void updateDiplomacy() {
        List<Faction> list = new ArrayList<>(factions.values());
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                Faction a = list.get(i), b = list.get(j);
                FactionStance cur = a.stanceToward(b.id);
                if (cur == FactionStance.WAR) continue;  // 진행 중 전쟁은 resolveWars에서
                int hostility = crossHostility(a, b);
                int aggrA = leaderTrait(a, Personality.Trait.AGGRESSION);
                int aggrB = leaderTrait(b, Personality.Trait.AGGRESSION);
                if (hostility >= WAR_HOSTILITY && (aggrA >= 50 || aggrB >= 50)
                        && cur != FactionStance.ALLY) {
                    declareWar(a, b);
                } else if (cur == FactionStance.NEUTRAL
                        && a.ideologySimilarity(b) > 0.7 && hostility == 0
                        && Rand.chance(0.2)) {
                    setStance(a, b, FactionStance.ALLY);
                    Bukkit.broadcastMessage("§a§l[동맹] §f" + a.name + " ↔ " + b.name);
                }
            }
        }
    }

    /** 두 세력 구성원 간 원한 관계 수 (한쪽이 다른 쪽을 원수로 둠). */
    private int crossHostility(Faction a, Faction b) {
        int count = 0;
        for (String am : a.members) {
            RebornNpc m = plugin.registry().get(am);
            if (m == null || m.soul == null) continue;
            for (String bm : b.members) {
                if (m.soul.nemeses.contains(bm) || m.soul.rivals.contains(bm)) count++;
            }
        }
        return count;
    }

    private void declareWar(Faction a, Faction b) {
        setStance(a, b, FactionStance.WAR);
        Bukkit.broadcastMessage("§c§l[선전포고] §f" + a.name + " §c⚔ §f" + b.name);
        assignWarGoals(a, b);
        assignWarGoals(b, a);
    }

    private void assignWarGoals(Faction self, Faction enemy) {
        for (String mid : self.members) {
            RebornNpc m = plugin.registry().get(mid);
            if (m == null || m.dead || m.soul == null) continue;
            // 적 지도자를 원수로 — relationToward 하락 + GoalGenerator가 DESTROY_RIVAL_FACTION 생성
            if (!m.soul.nemeses.contains(enemy.leaderId)) m.soul.nemeses.add(enemy.leaderId);
            // 호전적 구성원은 즉시 목표 부여
            if (m.soul.personality.get(Personality.Trait.AGGRESSION) >= 40
                    && m.goals.size() < GoalGenerator.MAX_GOALS) {
                boolean has = m.goals.stream().anyMatch(g -> g.kind == GoalKind.DESTROY_RIVAL_FACTION);
                if (!has) {
                    Goal g = new Goal(GoalKind.DESTROY_RIVAL_FACTION, enemy.id,
                            enemy.name + "을(를) 멸한다");
                    g.priority = 80;
                    m.goals.add(g);
                }
            }
        }
    }

    private void resolveWars() {
        for (Faction a : new ArrayList<>(factions.values())) {
            if (!factions.containsKey(a.id)) continue;  // 이번 주기에 흡수·해체됨
            for (var e : new HashMap<>(a.relations).entrySet()) {
                if (e.getValue() != FactionStance.WAR) continue;
                Faction b = factions.get(e.getKey());
                if (b == null) { a.relations.remove(e.getKey()); continue; }
                if (a.id.compareTo(b.id) > 0) continue;  // 쌍당 한 번만 처리
                double pa = a.power(plugin.registry()::get);
                double pb = b.power(plugin.registry()::get);
                if (a.size() == 0 || (b.size() > 0 && pa < pb * 0.4)) {
                    endWar(b, a);
                } else if (b.size() == 0 || pb < pa * 0.4) {
                    endWar(a, b);
                } else if (Rand.chance(0.05)) {
                    setStance(a, b, FactionStance.TRUCE);
                    Bukkit.broadcastMessage("§7[휴전] §f" + a.name + " — " + b.name);
                }
            }
        }
    }

    private void endWar(Faction winner, Faction loser) {
        Bukkit.broadcastMessage("§6§l[정복] §f" + winner.name + "이(가) "
                + loser.name + "을(를) 굴복시켰다!");
        if (loser.size() > 0 && Rand.chance(0.5)) {
            // 흡수 합병
            for (String mid : new HashSet<>(loser.members)) {
                RebornNpc m = plugin.registry().get(mid);
                if (m != null) { winner.members.add(mid); m.faction = winner.id; }
            }
            winner.treasury += loser.treasury;
            removeFaction(loser);
            winner.recomputeIdeology(plugin.registry()::get);
        } else {
            // 조공국으로 복속
            setStance(winner, loser, FactionStance.OVERLORD);
        }
    }

    // ───────────────────────── 수명주기 ─────────────────────────

    private void adoptPresetFactions() {
        Map<String, List<RebornNpc>> grouped = new HashMap<>();
        for (RebornNpc npc : plugin.registry().all()) {
            if (npc.dead || npc.faction.isEmpty() || factions.containsKey(npc.faction)) continue;
            grouped.computeIfAbsent(npc.faction, k -> new ArrayList<>()).add(npc);
        }
        for (var e : grouped.entrySet()) {
            List<RebornNpc> mem = e.getValue();
            if (mem.size() < 2) continue;  // 1명짜리는 세력으로 보지 않음
            RebornNpc leader = mem.stream()
                    .max(Comparator.comparingDouble(RebornNpc::effectiveTotal)).orElse(mem.get(0));
            Faction f = new Faction(e.getKey(), prettify(e.getKey()), leader.id);
            for (RebornNpc m : mem) f.members.add(m.id);
            f.territoryCenter = leader.location;
            f.recomputeIdeology(plugin.registry()::get);
            factions.put(f.id, f);
        }
    }

    private void updateLifecycle() {
        for (Faction f : new ArrayList<>(factions.values())) {
            f.members.removeIf(mid -> {
                RebornNpc m = plugin.registry().get(mid);
                return m == null || m.dead;
            });
            RebornNpc leader = plugin.registry().get(f.leaderId);
            if (leader == null || leader.dead || !f.members.contains(f.leaderId)) {
                electLeader(f);
            }
            if (f.size() <= 1) dissolve(f);
        }
    }

    private void electLeader(Faction f) {
        RebornNpc best = null;
        double bestScore = -1;
        for (String mid : f.members) {
            RebornNpc m = plugin.registry().get(mid);
            if (m == null || m.dead || m.soul == null) continue;
            double score = m.effectiveTotal()
                    + m.soul.personality.get(Personality.Trait.AMBITION)
                    + plugin.registry().socialNetwork().connectionCount(mid) * 5.0;
            if (score > bestScore) { bestScore = score; best = m; }
        }
        if (best != null && !best.id.equals(f.leaderId)) {
            f.leaderId = best.id;
            Bukkit.broadcastMessage("§e[세력 승계] §f" + f.name + "의 새 지도자: "
                    + clean(best.displayName));
        }
    }

    private void dissolve(Faction f) {
        for (String mid : f.members) {
            RebornNpc m = plugin.registry().get(mid);
            if (m != null && f.id.equals(m.faction)) m.faction = "";
        }
        removeFaction(f);
        Bukkit.broadcastMessage("§8[세력 와해] §7" + f.name + "이(가) 해체되었다.");
    }

    private void removeFaction(Faction f) {
        factions.remove(f.id);
        for (Faction other : factions.values()) other.relations.remove(f.id);
    }

    /** NPC 사망 시 호출 (NpcRegistry·NpcInteractListener). */
    public void onNpcDeath(RebornNpc npc) {
        if (npc.faction.isEmpty()) return;
        Faction f = factions.get(npc.faction);
        if (f == null) return;
        f.members.remove(npc.id);
        if (f.isLeader(npc.id)) electLeader(f);
        if (f.size() <= 1) dissolve(f);
    }

    // ───────────────────────── 보조 ─────────────────────────

    private int leaderTrait(Faction f, Personality.Trait t) {
        RebornNpc l = plugin.registry().get(f.leaderId);
        return (l != null && l.soul != null) ? l.soul.personality.get(t) : 0;
    }

    private static final String[] SUFFIX =
            {"가문", "일족", "기사단", "상회", "교단", "결사", "동맹", "왕국"};

    private String generateName(RebornNpc leader) {
        return clean(leader.displayName) + " " + SUFFIX[Rand.range(0, SUFFIX.length - 1)];
    }

    private String prettify(String id) { return id.replace('_', ' '); }

    private String clean(String s) { return s == null ? "" : s.replaceAll("§.", ""); }
}
