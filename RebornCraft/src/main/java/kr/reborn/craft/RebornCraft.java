package kr.reborn.craft;

import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Gui;
import kr.reborn.craft.command.AccessoryCommand;
import kr.reborn.craft.command.CraftCommand;
import kr.reborn.craft.command.CraftItemCommand;
import kr.reborn.craft.command.EnchantCommand;
import kr.reborn.craft.command.RepairCommand;
import kr.reborn.craft.listener.ConsumeListener;
import kr.reborn.craft.manager.AccessoryManager;
import kr.reborn.craft.manager.CraftingManager;
import kr.reborn.craft.manager.ItemRegistry;
import kr.reborn.craft.manager.ProficiencyManager;
import kr.reborn.craft.manager.RecipeRegistry;
import org.bukkit.plugin.java.JavaPlugin;

public final class RebornCraft extends JavaPlugin {

    private static RebornCraft instance;

    private ItemRegistry items;
    private RecipeRegistry recipes;
    private CraftingManager crafting;
    private ProficiencyManager proficiency;
    private AccessoryManager accessories;
    private Gui gui;

    public static RebornCraft get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (RebornCore.get() == null) {
            getLogger().severe("RebornCore가 비활성. 비활성화.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.gui = new Gui(this);
        this.items = new ItemRegistry(this);
        this.recipes = new RecipeRegistry(this);
        this.proficiency = new ProficiencyManager(this);
        this.crafting = new CraftingManager(this);
        this.accessories = new AccessoryManager(this);

        getCommand("craft").setExecutor(new CraftCommand(this));
        getCommand("accessory").setExecutor(new AccessoryCommand(this));
        getCommand("repair").setExecutor(new RepairCommand(this));
        getCommand("enchant").setExecutor(new EnchantCommand(this));
        getCommand("craftitem").setExecutor(new CraftItemCommand(this));

        getServer().getPluginManager().registerEvents(new ConsumeListener(this), this);

        getLogger().info("RebornCraft 활성화: 아이템 " + items.all().size() + "종 / 레시피 " + recipes.all().size() + "종");
    }

    @Override
    public void onDisable() {
        if (gui != null) gui.shutdown();
    }

    public ItemRegistry items() { return items; }
    public RecipeRegistry recipes() { return recipes; }
    public CraftingManager crafting() { return crafting; }
    public ProficiencyManager proficiency() { return proficiency; }
    public AccessoryManager accessories() { return accessories; }
    public Gui gui() { return gui; }
}
