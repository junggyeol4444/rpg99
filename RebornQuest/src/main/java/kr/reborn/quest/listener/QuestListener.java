package kr.reborn.quest.listener;

import kr.reborn.quest.RebornQuest;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

public final class QuestListener implements Listener {

    private final RebornQuest plugin;

    public QuestListener(RebornQuest p) { this.plugin = p; }

    @EventHandler
    public void onMobDeath(EntityDeathEvent e) {
        LivingEntity le = e.getEntity();
        Player killer = le.getKiller();
        if (killer == null) return;
        // 커스텀 몬스터 ID 추출 (RebornMob과 동일한 키 사용)
        String mobId = null;
        var rmob = plugin.getServer().getPluginManager().getPlugin("RebornMob");
        if (rmob != null) {
            mobId = le.getPersistentDataContainer()
                    .get(new NamespacedKey(rmob, "rmob"), PersistentDataType.STRING);
        }
        if (mobId == null) mobId = le.getType().name();
        plugin.engine().onKill(killer, mobId);
    }
}
