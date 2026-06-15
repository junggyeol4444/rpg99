package kr.reborn.worldai.ai;

import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Rand;
import kr.reborn.worldai.RebornWorldAI;
import kr.reborn.worldai.event.RebornDisasterStartEvent;
import kr.reborn.worldai.event.RebornWeatherChangeEvent;
import kr.reborn.worldai.event.RebornWorldAIAnalysisEvent;
import kr.reborn.worldai.event.RebornWorldAIDecisionEvent;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;

public final class WorldAI {

    private static final String NS = "RebornWorldAI.worldai";

    private final RebornWorldAI plugin;
    private final WorldKey world;

    private final Map<String, Long> questCooldowns = new HashMap<>();
    private final State state = new State();

    public WorldAI(RebornWorldAI plugin, WorldKey w) {
        this.plugin = plugin; this.world = w;
        loadState();
    }

    private void loadState() {
        try {
            String prefix = world.name() + ".";
            var all = kr.reborn.core.RebornCore.get().kv().loadAll(NS, null);
            for (var e : all.entrySet()) {
                if (!e.getKey().startsWith(prefix)) continue;
                String field = e.getKey().substring(prefix.length());
                try {
                    if (field.startsWith("cd.")) {
                        questCooldowns.put(field.substring(3), Long.parseLong(e.getValue()));
                    } else {
                        double v = Double.parseDouble(e.getValue());
                        switch (field) {
                            case "inflation" -> state.inflation = v;
                            case "tradeActivity" -> state.tradeActivity = v;
                            case "tension" -> state.tension = v;
                            case "stability" -> state.stability = v;
                            case "mobBalance" -> state.mobBalance = v;
                            default -> {}
                        }
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    public void saveState() {
        String prefix = world.name() + ".";
        var kv = kr.reborn.core.RebornCore.get().kv();
        kv.putDouble(NS, null, prefix + "inflation", state.inflation);
        kv.putDouble(NS, null, prefix + "tradeActivity", state.tradeActivity);
        kv.putDouble(NS, null, prefix + "tension", state.tension);
        kv.putDouble(NS, null, prefix + "stability", state.stability);
        kv.putDouble(NS, null, prefix + "mobBalance", state.mobBalance);
        for (var e : questCooldowns.entrySet()) {
            kv.putLong(NS, null, prefix + "cd." + e.getKey(), e.getValue());
        }
    }

    public WorldKey world() { return world; }
    public State state() { return state; }

    public void cycle() {
        processInbox();
        analyzeEconomy();
        analyzePolitics();
        analyzeMobs();
        analyzeWeather();
        propagateToNeighbors();
        coordinateGroup();
        decideQuests();
        directNpcs();
        directFactions();
        directMarket();
        // 스킬 창조 판단은 RebornSkill 측에서 자체 추적

        // 시대 판정 — 매 사이클
        try { plugin.epoch().cycle(world); } catch (Throwable ignored) {}

        Bukkit.getPluginManager().callEvent(new RebornWorldAIAnalysisEvent(world,
                state.inflation, state.tension, state.stability));
    }

    /**
     * 인박스 소비 — 이웃 AI가 보낸 메시지를 자기 state에 반영.
     *
     * TENSION_ALERT      +5 tension, -2 stability
     * WAR_DECLARATION    동맹(같은 연결권): +15 tension, -8 stability
     *                    적대(다른 연결권): +8 tension (대리전)
     * ECONOMY_REPORT     "boom": tradeActivity +0.15, inflation +5
     *                    "crash": tradeActivity -0.15, inflation -5
     * QUEST_LINK         +3 stability (목적 부여)
     * POLLUTION_ALERT    -0.15 mobBalance, -3 stability
     */
    private void processInbox() {
        var msgs = plugin.comm().drain(world);
        if (msgs.isEmpty()) return;
        for (var m : msgs) {
            switch (m.type) {
                case TENSION_ALERT -> {
                    state.tension = clamp(state.tension + 5, 0, 100);
                    state.stability = clamp(state.stability - 2, 0, 100);
                }
                case WAR_DECLARATION -> {
                    boolean ally = sameGroup(m.from, world);
                    if (ally) {
                        state.tension = clamp(state.tension + 15, 0, 100);
                        state.stability = clamp(state.stability - 8, 0, 100);
                    } else {
                        state.tension = clamp(state.tension + 8, 0, 100);
                    }
                }
                case ECONOMY_REPORT -> {
                    if ("boom".equals(m.payload)) {
                        state.tradeActivity = clamp(state.tradeActivity + 0.15, 0.1, 3.0);
                        state.inflation = clamp(state.inflation + 5, 50, 300);
                    } else if ("crash".equals(m.payload)) {
                        state.tradeActivity = clamp(state.tradeActivity - 0.15, 0.1, 3.0);
                        state.inflation = clamp(state.inflation - 5, 50, 300);
                    }
                }
                case QUEST_LINK -> {
                    state.stability = clamp(state.stability + 3, 0, 100);
                }
                case POLLUTION_ALERT -> {
                    state.mobBalance = clamp(state.mobBalance - 0.15, 0, 2.0);
                    state.stability = clamp(state.stability - 3, 0, 100);
                }
            }
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private boolean sameGroup(WorldKey a, WorldKey b) {
        for (var g : NEIGHBOR_GROUPS) {
            if (g.contains(a) && g.contains(b)) return true;
        }
        return false;
    }

    /**
     * 연결된 세계로 영향 전파 — 13세계가 격리되지 않도록.
     * 연결권(group1: FANTASY/DEMON/HEAVEN/SPIRIT, group2: MARTIAL/IMMORTAL/YOKAI)이
     * 같은 세계의 tension/inflation 미세 전이. WAR_ERA/DARK_AGE이면 더 강하게.
     */
    private void propagateToNeighbors() {
        var group = neighborsOf(world);
        if (group.isEmpty()) return;
        // 이 세계의 압력 = (tension - 30) / 100 (긴장이 평균보다 높으면 양수)
        double pressure = (state.tension - 30) / 100.0;
        // 시대 보정
        try {
            var ep = plugin.epoch().of(world);
            if (ep == kr.reborn.worldai.history.EpochManager.Epoch.DARK_AGE) pressure *= 2.5;
            else if (ep == kr.reborn.worldai.history.EpochManager.Epoch.WAR_ERA) pressure *= 1.7;
        } catch (Throwable ignored) {}
        // 인플레 영향
        double inflationLeak = (state.inflation - 100) / 200.0;
        for (var nw : group) {
            var ai = plugin.of(nw);
            if (ai == null) continue;
            // 긴장 전이 (절반 강도) — 한 세계 전쟁이 옆 세계에 풍문·난민 영향
            ai.state().tension = Math.max(0, Math.min(100,
                    ai.state().tension + pressure * 1.5));
            // 인플레 전이 — 무역 영향, 0.3배만
            ai.state().inflation = Math.max(50, Math.min(300,
                    ai.state().inflation + inflationLeak * 0.6));
        }
        // 추가 메시지 — 인박스 기반 협력 시그널
        var comm = plugin.comm();
        // 1. 경제 보고: 인플레 극단 시 이웃에 알림
        if (state.inflation > 200) {
            for (var nw : group) comm.send(world, nw,
                    kr.reborn.worldai.comm.AIComm.Type.ECONOMY_REPORT, "crash");
        } else if (state.inflation < 70 && state.tradeActivity > 1.5) {
            for (var nw : group) comm.send(world, nw,
                    kr.reborn.worldai.comm.AIComm.Type.ECONOMY_REPORT, "boom");
        }
        // 2. 오염 경보: 산업·종말 세계가 몹 폭주 시
        boolean polluter = world == WorldKey.APOCALYPSE || world == WorldKey.MAGITECH
                || world == WorldKey.CYBERPUNK;
        if (polluter && state.mobBalance > 1.5) {
            for (var nw : group) comm.send(world, nw,
                    kr.reborn.worldai.comm.AIComm.Type.POLLUTION_ALERT, "mb:" + state.mobBalance);
        }
    }

    /**
     * 연결권 단위 집단 행동 — 그룹 리더(가장 낮은 ordinal)만 실행.
     *
     * 황금기: 그룹 평균 tension<30 & stability>75 → 회원 stability +2, 역사 기록.
     * 암흑기 위험: 회원 2개 이상 stability<25 → 그룹 내 POLLUTION_ALERT 캐스케이드.
     * 대리전: 그룹 평균 tension>70 → 반대 연결권 리더에 TENSION_ALERT.
     */
    private void coordinateGroup() {
        var group = groupOf(world);
        if (group == null) return;
        WorldKey leader = leaderOf(group);
        if (world != leader) return;
        double sumT = 0, sumS = 0;
        int n = 0, lowStab = 0;
        for (WorldKey k : group) {
            var ai = plugin.of(k);
            if (ai == null) continue;
            sumT += ai.state().tension;
            sumS += ai.state().stability;
            if (ai.state().stability < 25) lowStab++;
            n++;
        }
        if (n < 2) return;
        double avgT = sumT / n, avgS = sumS / n;
        var comm = plugin.comm();
        if (avgT < 30 && avgS > 75) {
            for (WorldKey k : group) {
                var ai = plugin.of(k);
                if (ai != null) ai.state().stability =
                        clamp(ai.state().stability + 2, 0, 100);
            }
            try { plugin.history().record(world,
                    kr.reborn.worldai.history.WorldHistory.EventKind.SPECIAL,
                    "연결권 황금기 (그룹 평균 안정 " + (int) avgS + ")"); }
            catch (Throwable ignored) {}
            Bukkit.broadcastMessage("§6§l[황금기] §f" + world + " 연결권 §7- 평화·풍요 도래");
        }
        if (lowStab >= 2) {
            for (WorldKey k : group) {
                if (k == world) continue;
                comm.send(world, k,
                        kr.reborn.worldai.comm.AIComm.Type.POLLUTION_ALERT, "groupchaos");
            }
        }
        if (avgT > 70) {
            WorldKey enemyLeader = oppositeGroupLeader(group);
            if (enemyLeader != null) {
                comm.send(world, enemyLeader,
                        kr.reborn.worldai.comm.AIComm.Type.TENSION_ALERT, "proxy");
            }
        }
    }

    private static WorldKey leaderOf(java.util.Set<WorldKey> group) {
        WorldKey best = null;
        for (WorldKey k : group) {
            if (best == null || k.ordinal() < best.ordinal()) best = k;
        }
        return best;
    }

    private static java.util.Set<WorldKey> groupOf(WorldKey w) {
        for (var g : NEIGHBOR_GROUPS) if (g.contains(w)) return g;
        return null;
    }

    private WorldKey oppositeGroupLeader(java.util.Set<WorldKey> ownGroup) {
        for (var g : NEIGHBOR_GROUPS) {
            if (g != ownGroup) return leaderOf(g);
        }
        return null;
    }

    private static final java.util.List<java.util.Set<kr.reborn.core.data.WorldKey>> NEIGHBOR_GROUPS =
            java.util.List.of(
                    // group1: 판타지-마계-천계-정령계 연결권
                    java.util.EnumSet.of(kr.reborn.core.data.WorldKey.FANTASY,
                            kr.reborn.core.data.WorldKey.DEMON,
                            kr.reborn.core.data.WorldKey.HEAVEN,
                            kr.reborn.core.data.WorldKey.SPIRIT),
                    // group2: 무협-선계-요계 연결권
                    java.util.EnumSet.of(kr.reborn.core.data.WorldKey.MARTIAL,
                            kr.reborn.core.data.WorldKey.IMMORTAL,
                            kr.reborn.core.data.WorldKey.YOKAI));

    private java.util.List<kr.reborn.core.data.WorldKey> neighborsOf(kr.reborn.core.data.WorldKey self) {
        java.util.List<kr.reborn.core.data.WorldKey> out = new java.util.ArrayList<>();
        for (var group : NEIGHBOR_GROUPS) {
            if (!group.contains(self)) continue;
            for (var k : group) {
                if (k != self) out.add(k);
            }
        }
        return out;
    }

    private void directFactions() {
        try { plugin.factions().cycle(world, state.tension, state.stability); }
        catch (Throwable ignored) {}
    }

    private void directMarket() {
        try { plugin.market().cycle(world, state.tension, state.inflation); }
        catch (Throwable ignored) {}
    }

    private void analyzeEconomy() {
        // 1. 같은 세계 온라인 플레이어 수 추세 → 거래 활성도
        int worldPop = 0;
        for (var p : Bukkit.getOnlinePlayers()) {
            var d = kr.reborn.core.RebornCore.get().api().getPlayerData(p.getUniqueId());
            if (d != null && d.worldKey() == world) worldPop++;
        }
        // 인구 영향: pop > 5 → 거래 활성 ↑, pop < 2 → ↓
        double popDelta = worldPop >= 5 ? 0.08 : worldPop < 2 ? -0.05 : 0.0;
        state.tradeActivity = Math.max(0.1, Math.min(3.0,
                state.tradeActivity + popDelta + Rand.rangeD(-0.05, 0.05)));
        // 2. 거래 활성 ↑ → 인플레 ↑ (수요 증가)
        double tradeImpact = (state.tradeActivity - 1.0) * 2.0;
        state.inflation = Math.max(50, Math.min(300,
                state.inflation + tradeImpact + Rand.rangeD(-3, 3)));
    }

    private void analyzePolitics() {
        // 안정 ↓ → 긴장 ↑ (피드백 루프)
        double stabFb = (50 - state.stability) * 0.04;
        state.tension = Math.max(0, Math.min(100,
                state.tension + stabFb + Rand.rangeD(-2, 2)));
        // 긴장 ↑ → 안정 ↓
        double tensFb = -state.tension * 0.02;
        state.stability = Math.max(0, Math.min(100,
                state.stability + tensFb + Rand.rangeD(-1, 1)));
    }

    private void analyzeMobs() {
        // RebornMob의 같은 세계 활성 몹 수에 비례
        double active = 0.0;
        try {
            var mp = Bukkit.getPluginManager().getPlugin("RebornMob");
            if (mp != null) {
                Object ctl = mp.getClass().getMethod("controller").invoke(mp);
                Object cnt = ctl.getClass().getMethod("active").invoke(ctl);
                if (cnt instanceof Number n) active = n.doubleValue();
            }
        } catch (Throwable ignored) {}
        // 활성 몹이 50 이상 → mobBalance ↑, 10 이하 → ↓
        double mobDelta = active > 50 ? 0.03 : active < 10 ? -0.03 : 0.0;
        state.mobBalance = Math.max(0, Math.min(2.0,
                state.mobBalance + mobDelta + Rand.rangeD(-0.02, 0.02)));
    }

    private void analyzeWeather() {
        // 재해 확률은 상태에 비례 — 평시 1.5%, 긴장/암흑기엔 가속
        double base = plugin.getConfig().getDouble("thresholds.disaster-percent-per-cycle", 1.5) / 100.0;
        double tensionMult = 1.0 + (state.tension / 100.0) * 1.5;  // tension 100 → ×2.5
        double stabilityMult = 1.0 + ((100 - state.stability) / 100.0) * 1.0;
        // 시대 보정 (DARK_AGE / WAR_ERA에서 재해 빈번)
        double epochMult = 1.0;
        try {
            var ep = plugin.epoch().of(world);
            switch (ep) {
                case DARK_AGE -> epochMult = 3.0;
                case WAR_ERA -> epochMult = 2.0;
                case GOLDEN_AGE -> epochMult = 0.3;
                default -> {}
            }
        } catch (Throwable ignored) {}
        double pct = Math.min(0.5, base * tensionMult * stabilityMult * epochMult);
        if (Rand.chance(pct)) {
            String d = chooseDisasterForWorld();
            Bukkit.getPluginManager().callEvent(new RebornDisasterStartEvent(world, d, 600));
        }
        // 커스텀 날씨 — 안정 낮을수록 발생률 ↑
        double weatherPct = 0.01 + (100 - state.stability) / 5000.0;
        if (Rand.chance(weatherPct)) {
            var sec = plugin.getConfig().getConfigurationSection("weathers");
            if (sec != null) {
                java.util.List<String> candidates = new java.util.ArrayList<>();
                for (String key : sec.getKeys(false)) {
                    var s = sec.getConfigurationSection(key);
                    if (s != null && world.name().equals(s.getString("world"))) {
                        candidates.add(key);
                    }
                }
                if (!candidates.isEmpty()) {
                    String pick = candidates.get(Rand.range(0, candidates.size() - 1));
                    var s = sec.getConfigurationSection(pick);
                    Bukkit.getPluginManager().callEvent(
                            new RebornWeatherChangeEvent(world, pick, s.getInt("duration-min", 10)));
                }
            }
        }
    }

    /** 세계 특성에 맞는 재해 선택. */
    private String chooseDisasterForWorld() {
        return switch (world) {
            case FANTASY -> Rand.chance(0.5) ? "mana_burst" : "earthquake";
            case OCEAN -> "tsunami";
            case APOCALYPSE -> Rand.chance(0.5) ? "radiation_storm" : "ash_fall";
            case MAGITECH -> "ley_break";
            case DEMON -> "abyss_rift";
            case HEAVEN -> "celestial_storm";
            case SPIRIT -> "elemental_chaos";
            case DRAGON -> "dragonfire_rain";
            case EARTH -> "gate_break";
            case CYBERPUNK -> "blackout";
            case YOKAI -> "shadow_tide";
            case MARTIAL -> "qi_disturbance";
            case IMMORTAL -> "tribulation_lightning";
            default -> "earthquake";
        };
    }

    private void decideQuests() {
        long now = System.currentTimeMillis();
        if (state.tension > plugin.getConfig().getDouble("thresholds.political.war-tension", 80)) {
            tryQuest("WAR", now, "전쟁 임박");
        }
        if (state.inflation > plugin.getConfig().getDouble("thresholds.inflation.severe", 200)) {
            tryQuest("ECON_CRISIS", now, "경제 위기");
        }
        if (state.mobBalance > plugin.getConfig().getDouble("thresholds.mob.overflow-multiplier", 2.0)) {
            tryQuest("MOB_INVASION", now, "몬스터 침공");
        }
        if (state.stability > plugin.getConfig().getDouble("thresholds.political.festival-stability", 80)
                && state.tension < 20) {
            tryQuest("PEACE_FESTIVAL", now, "평화 축제");
        }
        // 세계별 특수 사건 — 낮은 확률로 자율 발동 (모든 월드 퀘스트가 시간에 따라 돌아간다)
        double special = plugin.getConfig().getDouble("thresholds.world-special-percent-per-cycle", 1.5) / 100.0;
        if (Rand.chance(special)) {
            tryQuest("WORLD_SPECIAL", now, worldSpecialLabel());
        }
        // 봉기·반란 — 안정도 낮을 때
        if (state.stability < 25 && Rand.chance(0.05)) {
            tryQuest("REVOLT", now, "봉기·반란");
        }
        // 보스 강림 — 매우 낮은 확률, 큰 사건
        if (Rand.chance(0.003)) {
            tryQuest("BOSS_DESCENT", now, "전설의 존재 강림");
        }
    }

    private String worldSpecialLabel() {
        switch (world) {
            case HEAVEN: return "타락 천사 반란 조짐";
            case SPIRIT: return "정령왕의 분노";
            case IMMORTAL: return "마선 대란";
            case YOKAI: return "백귀야행 대란";
            case EARTH: return "게이트 대폭주";
            case MAGITECH: return "마도 폭주";
            case APOCALYPSE: return "생존자 전쟁";
            case CYBERPUNK: return "기업 전쟁";
            case DRAGON: return "용왕의 시험";
            case OCEAN: return "바다의 왕 부활";
            case MARTIAL: return "비급 쟁탈전";
            case DEMON: return "신마전쟁";
            case FANTASY: return "차원의 틈 확장";
            default: return "세계 특수 사건";
        }
    }

    private void tryQuest(String key, long now, String label) {
        long cd = plugin.getConfig().getLong("quest-cooldowns." + key, 86400);
        long last = questCooldowns.getOrDefault(key, 0L);
        if (now - last < cd * 1000) return;
        questCooldowns.put(key, now);
        kr.reborn.core.RebornCore.get().kv().putLong(NS, null, world.name() + ".cd." + key, now);
        Bukkit.getPluginManager().callEvent(new RebornWorldAIDecisionEvent(world, key, label));
        Bukkit.broadcastMessage("§6[" + world + " AI] §f" + label + " 발동");
        // 역사 기록
        try {
            kr.reborn.worldai.history.WorldHistory.EventKind kind = switch (key) {
                case "WAR" -> kr.reborn.worldai.history.WorldHistory.EventKind.WAR_START;
                case "PEACE_FESTIVAL" -> kr.reborn.worldai.history.WorldHistory.EventKind.FESTIVAL;
                case "REVOLT" -> kr.reborn.worldai.history.WorldHistory.EventKind.REVOLT;
                case "BOSS_DESCENT" -> kr.reborn.worldai.history.WorldHistory.EventKind.BOSS_DESCENT;
                case "ECON_CRISIS" -> kr.reborn.worldai.history.WorldHistory.EventKind.ECON_CRISIS;
                default -> kr.reborn.worldai.history.WorldHistory.EventKind.SPECIAL;
            };
            plugin.history().record(world, kind, label);
        } catch (Throwable ignored) {}

        // RebornQuest hook — 같은 세계 플레이어 전원에게 월드 퀘스트 자동 부여
        try {
            var qPlugin = Bukkit.getPluginManager().getPlugin("RebornQuest");
            if (qPlugin == null) return;
            Object engine = qPlugin.getClass().getMethod("engine").invoke(qPlugin);
            // 키 → 퀘스트 ID 매핑
            String questId = mapKeyToQuestId(key);
            if (questId == null) return;
            for (var p : Bukkit.getOnlinePlayers()) {
                var data = kr.reborn.core.RebornCore.get().api().getPlayerData(p.getUniqueId());
                if (data == null) continue;
                if (data.worldKey() == world) {
                    engine.getClass().getMethod("accept", org.bukkit.entity.Player.class, String.class)
                            .invoke(engine, p, questId);
                }
            }
            // 다세계 연동 — comm 채널로 알림
            kr.reborn.worldai.comm.AIComm comm = plugin.comm();
            for (kr.reborn.core.data.WorldKey other : kr.reborn.core.data.WorldKey.values()) {
                if (other == world) continue;
                if (isLinkedRealm(world, other)) {
                    comm.send(world, other, kr.reborn.worldai.comm.AIComm.Type.QUEST_LINK,
                            "linked:" + key);
                }
            }
            // WAR 발동 — 동맹 참전 + 적 연결권 대리전 신호
            if ("WAR".equals(key)) {
                var group = groupOf(world);
                if (group != null) {
                    for (WorldKey ally : group) {
                        if (ally == world) continue;
                        comm.send(world, ally,
                                kr.reborn.worldai.comm.AIComm.Type.WAR_DECLARATION, label);
                    }
                    WorldKey enemyLeader = oppositeGroupLeader(group);
                    if (enemyLeader != null) {
                        comm.send(world, enemyLeader,
                                kr.reborn.worldai.comm.AIComm.Type.WAR_DECLARATION, label);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private String mapKeyToQuestId(String key) {
        switch (key) {
            case "WAR":
                switch (world) {
                    case FANTASY: return "marwang_invasion";
                    case DEMON: return "marwang_intermid_invasion";
                    case HEAVEN: return "shinma_war_heaven";
                    case MARTIAL: return "cult_appearance";
                    case OCEAN: return "great_sea_battle";
                    case CYBERPUNK: return "corp_war";
                    case APOCALYPSE: return "apoc_survivor_war";
                    case IMMORTAL: return "demon_immortal_chaos";
                    default: return null;
                }
            case "MOB_INVASION":
                switch (world) {
                    case YOKAI: return "yokai_king_revival";
                    case EARTH: return "gate_outbreak";
                    case DEMON: return "shinma_war_demon";
                    case OCEAN: return "sea_king_revival";
                    case DRAGON: return "dragon_king_revival";
                    default: return null;
                }
            case "WORLD_SPECIAL":
                switch (world) {
                    case HEAVEN: return "fallen_angel_rebellion";
                    case SPIRIT: return "spirit_king_rage";
                    case IMMORTAL: return "demon_immortal_chaos";
                    case YOKAI: return "hundred_demon_chaos";
                    case EARTH: return "gate_outbreak";
                    case MAGITECH: return "magitech_runaway";
                    case APOCALYPSE: return "apoc_survivor_war";
                    case CYBERPUNK: return "ai_liberation";
                    case DRAGON: return "dragon_king_revival";
                    case OCEAN: return "sea_king_revival";
                    case MARTIAL: return "bigeup_hunt";
                    case DEMON: return "shinma_war_demon";
                    case FANTASY: return "dimensional_rift";
                    default: return null;
                }
            case "REVOLT":
                switch (world) {
                    case CYBERPUNK: return "rebellion_revolution";
                    case MARTIAL: return "sapa_unification";
                    case IMMORTAL: return "cave_world_election";
                    case OCEAN: return "pirate_king_election";
                    default: return null;
                }
            case "BOSS_DESCENT":
                switch (world) {
                    case MARTIAL: return "cheonma_descent";
                    case EARTH: return "labyrinth_100f";
                    case FANTASY: return "marwang_invasion";
                    case DRAGON: return "dragon_king_revival";
                    case OCEAN: return "sea_king_revival";
                    default: return null;
                }
            case "ECON_CRISIS":
            case "PEACE_FESTIVAL":
            default: return null;
        }
    }

    /** 연결권 내 세계는 자동 연동. */
    private boolean isLinkedRealm(kr.reborn.core.data.WorldKey a, kr.reborn.core.data.WorldKey b) {
        java.util.Set<kr.reborn.core.data.WorldKey> g1 = java.util.Set.of(
                kr.reborn.core.data.WorldKey.FANTASY,
                kr.reborn.core.data.WorldKey.DEMON,
                kr.reborn.core.data.WorldKey.HEAVEN,
                kr.reborn.core.data.WorldKey.SPIRIT);
        java.util.Set<kr.reborn.core.data.WorldKey> g2 = java.util.Set.of(
                kr.reborn.core.data.WorldKey.MARTIAL,
                kr.reborn.core.data.WorldKey.IMMORTAL,
                kr.reborn.core.data.WorldKey.YOKAI);
        return (g1.contains(a) && g1.contains(b)) || (g2.contains(a) && g2.contains(b));
    }

    private void directNpcs() {
        try {
            plugin.simulator().cycle(world, state.tension, state.stability, state.mobBalance);
        } catch (Throwable ignored) {}
    }

    public static final class State {
        public double inflation = 100;
        public double tradeActivity = 1.0;
        public double tension = 30;
        public double stability = 60;
        public double mobBalance = 1.0;
    }
}
