package kr.reborn.tutorial.flow;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.tutorial.RebornTutorial;
import kr.reborn.tutorial.event.RebornTutorialStageChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

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
        Session(WorldKey w, long t, int stage) {
            this.world = w; this.startedAt = t; this.stage = stage;
        }
    }
}
