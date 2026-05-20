package kr.reborn.npc;

import kr.reborn.core.RebornCore;
import kr.reborn.npc.command.NpcCommand;
import kr.reborn.npc.entity.NpcRegistry;
import kr.reborn.npc.interact.NpcInteractListener;
import kr.reborn.npc.packet.PacketNpcController;
import org.bukkit.plugin.java.JavaPlugin;

public final class RebornNPC extends JavaPlugin {

    private static RebornNPC instance;
    private NpcRegistry registry;
    private PacketNpcController packetController;

    public static RebornNPC get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.registry = new NpcRegistry(this);
        this.packetController = new PacketNpcController(this);
        registry.loadAll();

        getCommand("rnpc").setExecutor(new NpcCommand(this));
        getServer().getPluginManager().registerEvents(new NpcInteractListener(this), this);

        long tick = getConfig().getLong("ai-tick-interval", 10L);
        RebornCore.get().scheduler().runTimer(registry::tickAll, tick, tick);
        // 패킷 가시성 갱신은 1초 간격
        RebornCore.get().scheduler().runTimer(packetController::updateVisibility, 20L, 20L);

        getLogger().info("RebornNPC 활성화");
    }

    @Override
    public void onDisable() {
        if (registry != null) registry.saveAll();
    }

    public NpcRegistry registry() { return registry; }
    public PacketNpcController packetController() { return packetController; }
}
