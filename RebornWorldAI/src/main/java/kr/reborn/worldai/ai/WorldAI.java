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

    private final RebornWorldAI plugin;
    private final WorldKey world;

    private final Map<String, Long> questCooldowns = new HashMap<>();
    private final State state = new State();

    public WorldAI(RebornWorldAI plugin, WorldKey w) {
        this.plugin = plugin; this.world = w;
    }

    public WorldKey world() { return world; }
    public State state() { return state; }

    public void cycle() {
        analyzeEconomy();
        analyzePolitics();
        analyzeMobs();
        analyzeWeather();
        decideQuests();
        directNpcs();
        // 스킬 창조 판단은 RebornSkill 측에서 자체 추적

        Bukkit.getPluginManager().callEvent(new RebornWorldAIAnalysisEvent(world,
                state.inflation, state.tension, state.stability));
    }

    private void analyzeEconomy() {
        // RebornEconomy hook (선택). 데이터 없으면 적당히 변동.
        state.inflation = Math.max(50, Math.min(300, state.inflation + Rand.rangeD(-5, 5)));
        state.tradeActivity = Math.max(0.1, Math.min(3.0, state.tradeActivity + Rand.rangeD(-0.1, 0.1)));
    }

    private void analyzePolitics() {
        state.tension = Math.max(0, Math.min(100, state.tension + Rand.rangeD(-3, 3)));
        state.stability = Math.max(0, Math.min(100, state.stability + Rand.rangeD(-2, 2)));
    }

    private void analyzeMobs() {
        state.mobBalance = Math.max(0, Math.min(2.0, state.mobBalance + Rand.rangeD(-0.05, 0.05)));
    }

    private void analyzeWeather() {
        double pct = plugin.getConfig().getDouble("thresholds.disaster-percent-per-cycle", 1.5) / 100.0;
        if (Rand.chance(pct)) {
            String[] disasters = {"earthquake", "tsunami", "mana_burst", "ley_break"};
            String d = disasters[Rand.range(0, disasters.length - 1)];
            Bukkit.getPluginManager().callEvent(new RebornDisasterStartEvent(world, d, 600));
        }
        // 커스텀 날씨 1% 확률
        if (Rand.chance(0.01)) {
            var sec = plugin.getConfig().getConfigurationSection("weathers");
            if (sec != null) {
                for (String key : sec.getKeys(false)) {
                    var s = sec.getConfigurationSection(key);
                    if (s != null && world.name().equals(s.getString("world"))) {
                        Bukkit.getPluginManager().callEvent(
                                new RebornWeatherChangeEvent(world, key, s.getInt("duration-min", 10)));
                        break;
                    }
                }
            }
        }
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
    }

    private void tryQuest(String key, long now, String label) {
        long cd = plugin.getConfig().getLong("quest-cooldowns." + key, 86400);
        long last = questCooldowns.getOrDefault(key, 0L);
        if (now - last < cd * 1000) return;
        questCooldowns.put(key, now);
        Bukkit.getPluginManager().callEvent(new RebornWorldAIDecisionEvent(world, key, label));
        // RebornQuest plugin에 hook (만약 활성화되어 있으면)
        Bukkit.broadcastMessage("§6[" + world + " AI] §f" + label + " 발동");
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
