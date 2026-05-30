package kr.reborn.npc;

import kr.reborn.core.RebornCore;
import kr.reborn.npc.command.NpcCommand;
import kr.reborn.npc.dialog.DialogueManager;
import kr.reborn.npc.dialog.DialogueRegistry;
import kr.reborn.npc.entity.NpcRegistry;
import kr.reborn.npc.interact.NpcInteractListener;
import kr.reborn.npc.packet.PacketNpcController;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class RebornNPC extends JavaPlugin {

    private static RebornNPC instance;
    private NpcRegistry registry;
    private PacketNpcController packetController;
    private DialogueRegistry dialogues;
    private DialogueManager dialogueManager;

    public static RebornNPC get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.registry = new NpcRegistry(this);
        this.packetController = new PacketNpcController(this);
        this.dialogues = new DialogueRegistry(this);
        this.dialogueManager = new DialogueManager(this);
        registry.loadAll();

        getCommand("rnpc").setExecutor(new NpcCommand(this));
        getServer().getPluginManager().registerEvents(new NpcInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new DialogueChatListener(), this);

        long tick = getConfig().getLong("ai-tick-interval", 10L);
        RebornCore.get().scheduler().runTimer(registry::tickAll, tick, tick);
        // 패킷 가시성 갱신은 1초 간격
        RebornCore.get().scheduler().runTimer(packetController::updateVisibility, 20L, 20L);

        getLogger().info("RebornNPC 활성화 — Dialogue " + dialogues.all().size() + "종");
    }

    @Override
    public void onDisable() {
        if (registry != null) registry.saveAll();
    }

    public NpcRegistry registry() { return registry; }
    public PacketNpcController packetController() { return packetController; }
    public DialogueRegistry dialogues() { return dialogues; }
    public DialogueManager dialogueManager() { return dialogueManager; }

    /** 채팅에 숫자만 입력 시 대화 선택지로 처리. */
    public final class DialogueChatListener implements Listener {
        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onChat(AsyncPlayerChatEvent e) {
            Player p = e.getPlayer();
            if (!dialogueManager.inSession(p.getUniqueId())) return;
            String msg = e.getMessage().trim();
            if (!msg.matches("[1-9]")) return;
            int idx = Integer.parseInt(msg) - 1;
            e.setCancelled(true);
            RebornCore.get().scheduler().runTask(() -> dialogueManager.choose(p, idx));
        }
    }
}
