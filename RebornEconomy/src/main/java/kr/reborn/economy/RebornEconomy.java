package kr.reborn.economy;

import kr.reborn.core.RebornCore;
import kr.reborn.core.util.Gui;
import kr.reborn.economy.command.AuctionCommand;
import kr.reborn.economy.command.ExchangeCommand;
import kr.reborn.economy.command.MailboxCommand;
import kr.reborn.economy.command.MoneyCommand;
import kr.reborn.economy.command.PayCommand;
import kr.reborn.economy.command.ShopCommand;
import kr.reborn.economy.command.TradeCommand;
import kr.reborn.economy.manager.AuctionManager;
import kr.reborn.economy.manager.CurrencyManager;
import kr.reborn.economy.manager.ExchangeManager;
import kr.reborn.economy.manager.MailboxManager;
import kr.reborn.economy.manager.ShopManager;
import kr.reborn.economy.manager.TradeManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class RebornEconomy extends JavaPlugin {

    private static RebornEconomy instance;

    private CurrencyManager currencies;
    private ExchangeManager exchange;
    private ShopManager shops;
    private AuctionManager auctions;
    private TradeManager trades;
    private MailboxManager mailbox;
    private Gui gui;

    public static RebornEconomy get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (RebornCore.get() == null) {
            getLogger().severe("RebornCore가 비활성 상태입니다. RebornEconomy 비활성화.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.gui = new Gui(this);
        this.currencies = new CurrencyManager(this);
        this.exchange = new ExchangeManager(this);
        this.shops = new ShopManager(this);
        this.auctions = new AuctionManager(this);
        this.trades = new TradeManager(this);
        this.mailbox = new MailboxManager(this);

        getCommand("money").setExecutor(new MoneyCommand(this));
        getCommand("pay").setExecutor(new PayCommand(this));
        getCommand("exchange").setExecutor(new ExchangeCommand(this));
        getCommand("shop").setExecutor(new ShopCommand(this));
        getCommand("auction").setExecutor(new AuctionCommand(this));
        getCommand("trade").setExecutor(new TradeCommand(this));
        getCommand("mailbox").setExecutor(new MailboxCommand(this));

        // 경매 만료 체크: 1분마다
        RebornCore.get().scheduler().runTimerAsync(() -> auctions.tickExpire(), 1200L, 1200L);

        getServer().getPluginManager().registerEvents(
                new kr.reborn.economy.listener.EconomyWorldImpactListener(this), this);

        getLogger().info("RebornEconomy 활성화: 통화 " + currencies.all().size() + "종");
    }

    @Override
    public void onDisable() {
        if (auctions != null) auctions.flush();
        if (mailbox != null) mailbox.flush();
        if (currencies != null) currencies.flush();
        if (gui != null) gui.shutdown();
    }

    public CurrencyManager currencies() { return currencies; }
    public ExchangeManager exchange() { return exchange; }
    public ShopManager shops() { return shops; }
    public AuctionManager auctions() { return auctions; }
    public TradeManager trades() { return trades; }
    public MailboxManager mailbox() { return mailbox; }
    public Gui gui() { return gui; }
}
