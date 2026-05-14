package kr.reborn.worldai;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.worldai.ai.WorldAI;
import kr.reborn.worldai.command.WorldAICommand;
import kr.reborn.worldai.comm.AIComm;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.Map;

public final class RebornWorldAI extends JavaPlugin {

    private static RebornWorldAI instance;
    private final Map<WorldKey, WorldAI> ais = new EnumMap<>(WorldKey.class);
    private AIComm comm;

    public static RebornWorldAI get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.comm = new AIComm(this);

        var sec = getConfig().getConfigurationSection("worlds");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                if (!sec.getBoolean(key)) continue;
                try {
                    WorldKey wk = WorldKey.valueOf(key);
                    ais.put(wk, new WorldAI(this, wk));
                } catch (Exception ignored) {}
            }
        }

        getCommand("worldai").setExecutor(new WorldAICommand(this));

        long tick = getConfig().getLong("analysis-tick-interval", 6000L);
        RebornCore.get().scheduler().runTimerAsync(this::tickAll, tick, tick);

        getLogger().info("RebornWorldAI 활성화 — " + ais.size() + "개 세계 AI");
    }

    private void tickAll() {
        for (WorldAI ai : ais.values()) {
            try { ai.cycle(); }
            catch (Exception e) { getLogger().warning("[" + ai.world() + "] 사이클 오류: " + e.getMessage()); }
        }
    }

    public WorldAI of(WorldKey w) { return ais.get(w); }
    public java.util.Collection<WorldAI> all() { return ais.values(); }
    public AIComm comm() { return comm; }
}
