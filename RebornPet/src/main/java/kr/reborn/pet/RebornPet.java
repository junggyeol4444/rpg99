package kr.reborn.pet;

import kr.reborn.pet.command.MountCommand;
import kr.reborn.pet.command.PetCommand;
import kr.reborn.pet.manager.ContractManager;
import kr.reborn.pet.manager.MountManager;
import kr.reborn.pet.manager.PetManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class RebornPet extends JavaPlugin {

    private static RebornPet instance;
    private PetManager pets;
    private MountManager mounts;
    private ContractManager contracts;

    public static RebornPet get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.pets = new PetManager(this);
        this.mounts = new MountManager(this);
        this.contracts = new ContractManager(this);

        getCommand("pet").setExecutor(new PetCommand(this));
        getCommand("mount").setExecutor(new MountCommand(this));

        getLogger().info("RebornPet 활성화");
    }

    public PetManager pets() { return pets; }
    public MountManager mounts() { return mounts; }
    public ContractManager contracts() { return contracts; }
}
