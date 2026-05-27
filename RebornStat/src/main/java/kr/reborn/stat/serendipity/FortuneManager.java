package kr.reborn.stat.serendipity;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.StatType;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import kr.reborn.stat.RebornStat;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 기연 발동 엔진. 채굴·사냥·탐험·운기 이벤트에서 세계별 기연을 굴려, 발동 시 영구 보상 지급.
 * 획득 여부는 PlayerData.status에 영구 마커("fortune:<id>")로 저장하여 중복 획득을 막는다.
 */
public final class FortuneManager implements Listener {

    private final RebornStat plugin;
    private final FortuneRegistry registry;
    /** 플레이어별 마지막으로 기연을 굴린 청크 (탐험 트리거 과다 호출 방지). */
    private final Map<UUID, Long> lastExploreChunk = new HashMap<>();

    public FortuneManager(RebornStat plugin, FortuneRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @EventHandler
    public void onMine(BlockBreakEvent e) {
        roll(e.getPlayer(), Fortune.Trigger.MINE, e.getBlock().getType().name());
    }

    @EventHandler
    public void onKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        roll(killer, Fortune.Trigger.KILL, mobId(e.getEntity()));
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo() == null) return;
        int fx = e.getFrom().getBlockX() >> 4, fz = e.getFrom().getBlockZ() >> 4;
        int tx = e.getTo().getBlockX() >> 4, tz = e.getTo().getBlockZ() >> 4;
        if (fx == tx && fz == tz) return;  // 같은 청크 — 99.9%는 여기서 종료
        long key = ((long) tx << 32) ^ (tz & 0xffffffffL);
        Long prev = lastExploreChunk.get(e.getPlayer().getUniqueId());
        if (prev != null && prev == key) return;
        lastExploreChunk.put(e.getPlayer().getUniqueId(), key);
        roll(e.getPlayer(), Fortune.Trigger.EXPLORE, "");
    }

    /** /meditate(운기조식)에서 호출. */
    public void rollMeditate(Player p) { roll(p, Fortune.Trigger.MEDITATE, ""); }

    private void roll(Player p, Fortune.Trigger trigger, String param) {
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        if (d == null) return;
        for (Fortune f : registry.forWorld(d.worldKey())) {
            if (f.trigger != trigger) continue;
            if (!f.param.isEmpty() && !f.param.equalsIgnoreCase(param)) continue;
            if (has(d, f.id)) continue;
            if (!Rand.chance(f.chance)) continue;
            grant(p, d, f);
            return;  // 한 번에 하나만
        }
    }

    /** 기연 보상 지급 (이미 얻었으면 false). 관리/테스트 명령에서도 호출. */
    public boolean grant(Player p, PlayerData d, Fortune f) {
        if (has(d, f.id)) return false;
        for (Map.Entry<StatType, Double> e : f.statRewards.entrySet()) {
            RebornCore.get().api().addStat(p.getUniqueId(), e.getKey(), e.getValue(), "fortune:" + f.id);
        }
        if (!f.skillReward.isEmpty()) grantSkill(p.getUniqueId(), f.skillReward);
        mark(d, f.id);
        RebornCore.get().tierManager().checkAndAdvance(p, d);  // 기연으로 경지 상승 가능
        Msg.send(p, "&6&l[기연] &r&e" + f.name);
        if (!f.message.isEmpty()) Msg.send(p, "&7" + f.message);
        if (f.broadcast) {
            Bukkit.broadcastMessage("§6§l[기연] §f" + p.getName()
                    + "이(가) §e" + stripColor(f.name) + "§f의 기연을 얻었다!");
        }
        p.getWorld().playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        return true;
    }

    private void grantSkill(UUID uuid, String skillId) {
        try {
            var sp = Bukkit.getPluginManager().getPlugin("RebornSkill");
            if (sp != null) {
                sp.getClass().getMethod("learnByApi", UUID.class, String.class).invoke(sp, uuid, skillId);
            }
        } catch (Throwable ignored) {}
    }

    public boolean has(PlayerData d, String id) { return d.getStatus("fortune:" + id) != null; }

    private void mark(PlayerData d, String id) {
        d.status().put("fortune:" + id,
                new PlayerData.StatusEffect("fortune:" + id, "FORTUNE", Long.MAX_VALUE, 1));
        d.markDirty();
    }

    private String mobId(LivingEntity le) {
        var rmob = Bukkit.getPluginManager().getPlugin("RebornMob");
        if (rmob != null) {
            String id = le.getPersistentDataContainer()
                    .get(new NamespacedKey(rmob, "rmob"), PersistentDataType.STRING);
            if (id != null) return id;
        }
        return le.getType().name();
    }

    private String stripColor(String s) { return s == null ? "" : s.replaceAll("&.|§.", ""); }
}
