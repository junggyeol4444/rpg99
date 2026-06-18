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
    private kr.reborn.npc.famous.FamousNpcRegistry famous;
    private kr.reborn.npc.famous.FamousEncounter famousEncounter;

    public static RebornNPC get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.registry = new NpcRegistry(this);
        this.packetController = new PacketNpcController(this);
        this.dialogues = new DialogueRegistry(this);
        this.dialogueManager = new DialogueManager(this);
        this.famous = new kr.reborn.npc.famous.FamousNpcRegistry(this);
        this.famousEncounter = new kr.reborn.npc.famous.FamousEncounter(this);
        registry.loadAll();

        getCommand("rnpc").setExecutor(new NpcCommand(this));
        if (getCommand("famous") != null) {
            getCommand("famous").setExecutor(
                    new kr.reborn.npc.command.FamousCommand(this));
        }
        getServer().getPluginManager().registerEvents(new NpcInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new DialogueChatListener(), this);
        getServer().getPluginManager().registerEvents(registry.questOffers(), this);

        long tick = getConfig().getLong("ai-tick-interval", 10L);
        RebornCore.get().scheduler().runTimer(registry::tickAll, tick, tick);
        // 패킷 가시성 갱신은 1초 간격
        RebornCore.get().scheduler().runTimer(packetController::updateVisibility, 20L, 20L);
        // 5초 뒤 Famous NPC 자동 spawn (다른 월드 로드 대기)
        RebornCore.get().scheduler().runTaskLater(() -> {
            try { famous.spawnAllToWorld(); } catch (Throwable ignored) {}
        }, 100L);
        // 5분마다 자동 영속화 — 서버 크래시 시 NPC 상태·세력 데이터 손실 방지
        RebornCore.get().scheduler().runTimerAsync(() -> {
            if (registry != null) {
                try { registry.saveAll(); } catch (Throwable ignored) {}
                if (registry.factions() != null) {
                    try { registry.factions().saveAll(); } catch (Throwable ignored) {}
                }
            }
        }, 6000L, 6000L);

        getLogger().info("RebornNPC 활성화 — Dialogue " + dialogues.all().size()
                + "종, Famous " + famous.all().size() + "체");
    }

    @Override
    public void onDisable() {
        if (registry != null) {
            registry.saveAll();
            if (registry.factions() != null) registry.factions().saveAll();
        }
    }

    public NpcRegistry registry() { return registry; }
    public PacketNpcController packetController() { return packetController; }
    public DialogueRegistry dialogues() { return dialogues; }
    public DialogueManager dialogueManager() { return dialogueManager; }
    public kr.reborn.npc.famous.FamousNpcRegistry famous() { return famous; }
    public kr.reborn.npc.famous.FamousEncounter famousEncounter() { return famousEncounter; }

    /**
     * 외부 호출 API — 반경 내 같은 faction NPC에 임시 직속 명령.
     * HiddenAbility(CULT_COMMAND/YOKAI_EMPEROR/AI_COMMAND/SEA_KING_COMMAND)가 리플렉션으로 호출.
     */
    public int commandNearbyNpcs(Player p, double radius, long durationMs, String factionFilter) {
        int affected = 0;
        long expireAt = System.currentTimeMillis() + durationMs;
        for (var npc : registry.all()) {
            if (npc.dead) continue;
            if (npc.location == null) continue;
            if (npc.location.getWorld() != p.getWorld()) continue;
            if (npc.location.distanceSquared(p.getLocation()) > radius * radius) continue;
            if (factionFilter != null && !factionFilter.isEmpty()) {
                if (npc.faction == null || !npc.faction.toUpperCase().contains(factionFilter.toUpperCase())) continue;
            }
            // 임시 직속 표시 — aiData에 저장
            npc.aiData.put("commanded_by", p.getUniqueId());
            npc.aiData.put("command_until", expireAt);
            // 호감도 +50 (강제 충성)
            npc.relations.addPlayer(p.getUniqueId(), 50);
            affected++;
        }
        return affected;
    }

    /**
     * 외부 호출 API — 반경 내 NPC들의 호감도 일괄 조정.
     * HiddenAbility(HERO_AURA), Curse(NPC_FAVOR_TICK), MartialSchool 학파 변경 시 호출.
     */
    public int nudgeNearbyFavor(Player p, double radius, double delta) {
        int affected = 0;
        for (var npc : registry.all()) {
            if (npc.dead || npc.location == null) continue;
            if (npc.location.getWorld() != p.getWorld()) continue;
            if (npc.location.distanceSquared(p.getLocation()) > radius * radius) continue;
            npc.relations.addPlayer(p.getUniqueId(), delta);
            affected++;
        }
        return affected;
    }

    /**
     * 외부 호출 API — 모든 NPC 호감도 일괄 조정 (전 플레이어 대상 기본 모드 ±).
     * Calendar 신년 이벤트가 호출.
     */
    public int nudgeGlobalFavor(double delta) {
        int affected = 0;
        for (var npc : registry.all()) {
            if (npc.dead) continue;
            npc.relations.adjustGlobalMood(delta);
            affected++;
        }
        return affected;
    }

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
