package kr.reborn.tutorial.flow;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.tutorial.RebornTutorial;
import kr.reborn.tutorial.event.RebornTutorialStageChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 튜토리얼 진행 관리. 단계별 시간 추적. */
public final class TutorialManager implements Listener {

    private final RebornTutorial plugin;
    private final ConcurrentHashMap<UUID, Session> sessions = new ConcurrentHashMap<>();

    public TutorialManager(RebornTutorial p) { this.plugin = p; }

    public void start(UUID id) {
        var p = Bukkit.getPlayer(id);
        if (p == null) return;
        WorldKey w;
        try { w = WorldKey.valueOf(p.getWorld().getName().toUpperCase().replace("TUTORIAL_", "")); }
        catch (Exception e) { return; }
        Session s = new Session(w, System.currentTimeMillis(), 1);
        sessions.put(id, s);
        Msg.send(p, "&e튜토리얼 단계 1: 기본 조작 학습. 보호 구역 내에서 자유롭게 둘러봐라.");
        grantStarterSkills(p, w);
    }

    public void tick() {
        long now = System.currentTimeMillis();
        long s1 = plugin.getConfig().getLong("stage1-minutes", 10) * 60_000L;
        long s2 = plugin.getConfig().getLong("stage2-minutes", 10) * 60_000L;
        for (var e : sessions.entrySet()) {
            Session s = e.getValue();
            long elapsed = now - s.startedAt;
            int newStage = s.stage;
            if (elapsed > s1 && s.stage < 2) newStage = 2;
            if (elapsed > s1 + s2 && s.stage < 3) newStage = 3;
            if (newStage != s.stage) {
                s.stage = newStage;
                var p = Bukkit.getPlayer(e.getKey());
                if (p != null) {
                    Bukkit.getPluginManager().callEvent(new RebornTutorialStageChangeEvent(p, newStage));
                    Msg.send(p, "&6튜토리얼 단계 " + newStage + "로 진입.");
                    if (newStage == 3) Msg.send(p, "&a본서버 이동 포탈 활성화. /rtut exit 으로 본 세계로 진입.");
                }
            }
        }
    }

    public void exitToMain(UUID id) {
        Session s = sessions.remove(id);
        if (s == null) return;
        var p = Bukkit.getPlayer(id);
        if (p == null) return;
        var w = Bukkit.getWorld(s.world.name().toLowerCase());
        if (w == null) { Msg.error(p, "본 세계가 없습니다."); return; }
        p.teleport(w.getSpawnLocation());
        Msg.send(p, "&6본 세계 진입: " + s.world);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        var p = e.getPlayer();
        var name = p.getWorld().getName();
        if (name.startsWith("tutorial")) start(p.getUniqueId());
    }

    private final Map<UUID, Long> trainCooldown = new HashMap<>();

    /** 기획서 4장 — 튜토리얼 사망 시 선택지(심부름꾼/명계). NPC에게는 죽지 않으므로 환경/이탈 사망만. */
    @EventHandler
    public void onTutorialDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        Session s = sessions.get(p.getUniqueId());
        if (s == null) return;
        e.setKeepInventory(true);
        e.setKeepLevel(true);
        e.getDrops().clear();
        s.awaitingChoice = true;
        promptChoice(p);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Session s = sessions.get(e.getPlayer().getUniqueId());
        if (s == null || !s.awaitingChoice) return;
        World lobby = Bukkit.getWorld("lobby");
        if (lobby != null) e.setRespawnLocation(lobby.getSpawnLocation());
        RebornCore.get().scheduler().runTaskLater(() -> {
            if (e.getPlayer().isOnline()) promptChoice(e.getPlayer());
        }, 10L);
    }

    private void promptChoice(Player p) {
        Msg.send(p, "&7튜토리얼에서 쓰러졌다. 어디로 갈 것인가?");
        Msg.send(p, "&e/rtut choose errand &7— 신들의 심부름꾼이 되어 환생의 월드에서 산다");
        Msg.send(p, "&5/rtut choose underworld &7— 명계로 끌려가 명계에서 산다");
    }

    /** errand(심부름꾼) 또는 underworld(명계) 선택 처리. */
    public void resolveChoice(UUID id, String choice) {
        Session s = sessions.get(id);
        Player p = Bukkit.getPlayer(id);
        if (s == null || !s.awaitingChoice || p == null) return;
        if ("underworld".equalsIgnoreCase(choice)) {
            World uw = Bukkit.getWorld("underworld");
            if (uw != null) p.teleport(uw.getSpawnLocation());
            Msg.send(p, "&5명계로 끌려갔다. 이곳에서 새 삶이 시작된다.");
        } else {
            World lobby = Bukkit.getWorld("lobby");
            if (lobby != null) p.teleport(lobby.getSpawnLocation());
            Msg.send(p, "&b신들의 심부름꾼이 되었다. 환생의 월드에서 새 기회를 기다려라.");
        }
        s.awaitingChoice = false;
        sessions.remove(id);
    }

    /** 기획서 4장 — 튜토리얼 NPC 수련. 보호 구역에서 수련해 본 세계보다 강한 스탯을 쌓는다. */
    public void train(Player p) {
        Session s = sessions.get(p.getUniqueId());
        if (s == null) { Msg.warn(p, "튜토리얼 중에만 수련할 수 있다."); return; }
        long now = System.currentTimeMillis();
        long cd = plugin.getConfig().getLong("train-cooldown-seconds", 30) * 1000L;
        Long next = trainCooldown.get(p.getUniqueId());
        if (next != null && now < next) {
            Msg.warn(p, "수련 회복 중 — " + (next - now) / 1000 + "초");
            return;
        }
        trainCooldown.put(p.getUniqueId(), now + cd);
        int amt = plugin.getConfig().getInt("train-stat-gain", 2);
        try {
            for (var st : kr.reborn.core.data.StatType.COMMON_8) {
                RebornCore.get().api().addStat(p.getUniqueId(), st, amt, "tutorial-train");
            }
            Msg.send(p, "&a수련으로 모든 기본 능력치 +" + amt);
        } catch (Throwable t) {
            Msg.error(p, "수련에 실패했다.");
        }
    }

    private void grantStarterSkills(org.bukkit.entity.Player p, WorldKey w) {
        var list = plugin.getConfig().getStringList("starter-skills." + w.name());
        for (String skillId : list) {
            // RebornSkill API hook (런타임에 RebornSkill 활성 시만 동작)
            try {
                var rs = Bukkit.getPluginManager().getPlugin("RebornSkill");
                if (rs != null) {
                    rs.getClass().getMethod("learnByApi", UUID.class, String.class)
                            .invoke(rs, p.getUniqueId(), skillId);
                }
            } catch (Exception ignored) {}
        }
        if (!list.isEmpty()) Msg.send(p, "&b스킬 " + list.size() + "개 자동 지급.");
    }

    public Session sessionOf(UUID id) { return sessions.get(id); }

    public static final class Session {
        public final WorldKey world;
        public final long startedAt;
        public int stage;
        public boolean awaitingChoice;
        Session(WorldKey w, long t, int stage) {
            this.world = w; this.startedAt = t; this.stage = stage;
        }
    }
}
