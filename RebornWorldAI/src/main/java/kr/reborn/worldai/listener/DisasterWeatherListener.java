package kr.reborn.worldai.listener;

import kr.reborn.worldai.RebornWorldAI;
import kr.reborn.worldai.event.RebornDisasterStartEvent;
import kr.reborn.worldai.event.RebornWeatherChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * AI가 발생시킨 재해·날씨 이벤트를 받아 실제 엔진에 위임.
 *
 * WorldAI 자체는 이벤트만 callEvent하고 적용은 이 listener를 통해 분리.
 */
public final class DisasterWeatherListener implements Listener {

    private final RebornWorldAI plugin;

    public DisasterWeatherListener(RebornWorldAI plugin) { this.plugin = plugin; }

    @EventHandler
    public void onDisaster(RebornDisasterStartEvent e) {
        plugin.disasters().start(e.world, e.disasterType, e.durationSeconds);
    }

    @EventHandler
    public void onWeather(RebornWeatherChangeEvent e) {
        plugin.weather().start(e.world, e.weather, e.durationMinutes);
    }
}
