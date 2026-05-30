package kr.reborn.tutorial.quest;

import kr.reborn.tutorial.RebornTutorial;
import kr.reborn.tutorial.event.RebornTutorialStageChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class HintStageListener implements Listener {

    private final RebornTutorial plugin;

    public HintStageListener(RebornTutorial plugin) { this.plugin = plugin; }

    @EventHandler
    public void onStageChange(RebornTutorialStageChangeEvent e) {
        switch (e.stage()) {
            case 1 -> plugin.hints().stage1Hints(e.getPlayer());
            case 2 -> plugin.hints().stage2Hints(e.getPlayer());
            case 3 -> plugin.hints().stage3Hints(e.getPlayer());
        }
    }
}
