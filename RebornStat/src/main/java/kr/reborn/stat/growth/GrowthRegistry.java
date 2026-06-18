package kr.reborn.stat.growth;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.event.RebornQuestCompleteEvent;
import kr.reborn.core.util.Msg;
import kr.reborn.stat.RebornStat;
import kr.reborn.stat.growth.impl.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.EnumMap;
import java.util.Map;

public final class GrowthRegistry implements Listener {

    private final RebornStat plugin;
    private final Map<WorldKey, GrowthStrategy> strategies = new EnumMap<>(WorldKey.class);

    public GrowthRegistry(RebornStat plugin) {
        this.plugin = plugin;
        register(new FantasyGrowth());
        register(new MartialGrowth());
        register(new YokaiGrowth());
        register(new SpiritGrowth());
        register(new EarthGrowth());
        register(new DragonGrowth());
        register(new ImmortalGrowth());
        register(new DemonGrowth());
        register(new HeavenGrowth());
        register(new MagitechGrowth());
        register(new ApocalypseGrowth());
        register(new CyberpunkGrowth());
        register(new OceanGrowth());
    }

    public void register(GrowthStrategy s) { strategies.put(s.world(), s); }

    public GrowthStrategy of(WorldKey w) { return strategies.get(w); }

    @EventHandler
    public void onMobDeath(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        var d = RebornCore.get().api().getPlayerData(killer.getUniqueId());
        if (d == null) return;
        GrowthStrategy s = of(d.worldKey());
        if (s == null) return;
        s.onMonsterKill(killer, d, e.getEntity().getMaxHealth());
        RebornCore.get().tierManager().checkAndAdvance(killer, d);
    }

    @EventHandler
    public void onQuest(RebornQuestCompleteEvent e) {
        var d = RebornCore.get().api().getPlayerData(e.getPlayer().getUniqueId());
        if (d == null) return;
        GrowthStrategy s = of(d.worldKey());
        if (s != null) s.onQuestComplete(e.getPlayer(), d, 1.0);
        RebornCore.get().tierManager().checkAndAdvance(e.getPlayer(), d);
    }

    /** /meditate 등에서 직접 호출 */
    public void meditate(Player p, double quality) {
        var d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (d == null) return;
        GrowthStrategy s = of(d.worldKey());
        if (s == null) {
            Msg.warn(p, "이 세계에서는 운기조식이 통하지 않는다.");
            return;
        }
        s.onMeditate(p, d, quality);
        RebornCore.get().tierManager().checkAndAdvance(p, d);
        if (plugin.fortuneManager() != null) plugin.fortuneManager().rollMeditate(p);  // 운기 중 기연
    }
}
