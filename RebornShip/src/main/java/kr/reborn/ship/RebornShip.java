package kr.reborn.ship;

import kr.reborn.ship.command.ShipCommand;
import kr.reborn.ship.manager.ShipMovement;
import kr.reborn.ship.manager.ShipRegistry;
import org.bukkit.plugin.java.JavaPlugin;

public final class RebornShip extends JavaPlugin {

    private static RebornShip instance;
    private ShipRegistry ships;
    private ShipMovement movement;
    private kr.reborn.ship.combat.ShipCombat combat;

    public static RebornShip get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.ships = new ShipRegistry(this);
        this.movement = new ShipMovement(this);
        this.combat = new kr.reborn.ship.combat.ShipCombat(this);
        getCommand("ship").setExecutor(new ShipCommand(this));
        // 5분마다 자동 저장 (크래시 안전망)
        kr.reborn.core.RebornCore.get().scheduler().runTimerAsync(
                () -> ships.saveAll(), 6000L, 6000L);
        getLogger().info("RebornShip 활성화 — 해전 시스템 가동 (배 "
                + ships.all().size() + "척 복원)");
    }

    @Override
    public void onDisable() {
        if (ships != null) ships.saveAll();
    }

    public ShipRegistry ships() { return ships; }
    public ShipMovement movement() { return movement; }
    public kr.reborn.ship.combat.ShipCombat combat() { return combat; }
}
