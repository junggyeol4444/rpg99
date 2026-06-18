package kr.reborn.quest.listener;

import kr.reborn.core.event.RebornNPCInteractEvent;
import kr.reborn.quest.RebornQuest;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * 게임 사건을 QuestEngine.progress()로 연결.
 * 각 핸들러는 "무슨 타입의 어떤 대상 사건이 일어났다"만 알려주고, 진행 판정은 엔진이 한다.
 */
public final class QuestListener implements Listener {

    private final RebornQuest plugin;

    public QuestListener(RebornQuest p) { this.plugin = p; }

    /** KILL — 커스텀 몹 id 우선, 없으면 바닐라 EntityType 이름. */
    @EventHandler
    public void onMobDeath(EntityDeathEvent e) {
        LivingEntity le = e.getEntity();
        Player killer = le.getKiller();
        if (killer == null) return;
        String mobId = null;
        var rmob = plugin.getServer().getPluginManager().getPlugin("RebornMob");
        if (rmob != null) {
            mobId = le.getPersistentDataContainer()
                    .get(new NamespacedKey(rmob, "rmob"), PersistentDataType.STRING);
        }
        if (mobId == null) mobId = le.getType().name();
        plugin.engine().progress(killer, "KILL", mobId, 1);
    }

    /** GATHER — 아이템 획득. */
    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        ItemStack item = e.getItem().getItemStack();
        plugin.engine().progress(p, "GATHER", item.getType().name(), item.getAmount());
    }

    /** CRAFT — 제작. */
    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemStack result = e.getRecipe().getResult();
        plugin.engine().progress(p, "CRAFT", result.getType().name(), Math.max(1, result.getAmount()));
    }

    /** TALK — RebornNPC 상호작용. */
    @EventHandler
    public void onTalk(RebornNPCInteractEvent e) {
        plugin.engine().progress(e.getPlayer(), "TALK", e.npcId(), 1);
    }

    /** EXPLORE — 다른 월드 진입. */
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        plugin.engine().progress(e.getPlayer(), "EXPLORE", e.getPlayer().getWorld().getName(), 1);
    }

    /** SURVIVE — 사망 시 진행 초기화. */
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        plugin.engine().onPlayerDeath(e.getEntity());
    }
}
