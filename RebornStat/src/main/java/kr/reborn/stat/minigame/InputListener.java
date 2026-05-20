package kr.reborn.stat.minigame;

import kr.reborn.stat.RebornStat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;

/**
 * 바닐라 입력을 운기조식 미니게임으로 전달.
 * Sneak (S), Sprint (R), Jump (J)
 */
public final class InputListener implements Listener {

    private final RebornStat plugin;

    public InputListener(RebornStat p) { this.plugin = p; }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) return;
        plugin.minigames().recordMeditationInput(e.getPlayer(), "S");
    }

    @EventHandler
    public void onSprint(PlayerToggleSprintEvent e) {
        if (!e.isSprinting()) return;
        plugin.minigames().recordMeditationInput(e.getPlayer(), "R");
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        // Jump 탐지: Y 증가량이 1블록 가까이 + on ground였음
        if (e.getFrom().getY() < e.getTo().getY()
                && (e.getTo().getY() - e.getFrom().getY()) > 0.41
                && e.getFrom().getY() == Math.floor(e.getFrom().getY())) {
            plugin.minigames().recordMeditationInput(e.getPlayer(), "J");
        }
    }
}
