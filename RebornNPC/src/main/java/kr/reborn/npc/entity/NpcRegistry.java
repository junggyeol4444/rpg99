package kr.reborn.npc.entity;

import kr.reborn.core.data.WorldKey;
import kr.reborn.npc.RebornNPC;
import kr.reborn.npc.ai.NpcBrain;
import kr.reborn.npc.ai.SimpleAI;
import kr.reborn.npc.ai.behavior.ChildbirthBehavior;
import kr.reborn.npc.ai.behavior.CombatBehavior;
import kr.reborn.npc.ai.behavior.FleeBehavior;
import kr.reborn.npc.ai.behavior.IdleBehavior;
import kr.reborn.npc.ai.behavior.PatrolBehavior;
import kr.reborn.npc.ai.behavior.RevengeBehavior;
import kr.reborn.npc.ai.behavior.ScheduleBehavior;
import kr.reborn.npc.ai.behavior.SocialBehavior;
import kr.reborn.npc.emotion.Emotion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Mob;

import java.io.File;
import java.util.EnumMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NpcRegistry {

    private final RebornNPC plugin;
    private final ConcurrentHashMap<String, RebornNpc> byId = new ConcurrentHashMap<>();
    private final EnumMap<Emotion.Kind, Double> decayRates = new EnumMap<>(Emotion.Kind.class);
    private final kr.reborn.npc.soul.GoalGenerator goalGenerator;
    private final kr.reborn.npc.soul.GoalProgressor goalProgressor;
    private final kr.reborn.npc.social.SocialNetwork socialNetwork = new kr.reborn.npc.social.SocialNetwork();
    private final kr.reborn.npc.social.GossipManager gossip;

    public NpcRegistry(RebornNPC plugin) {
        this.plugin = plugin;
        this.goalGenerator = new kr.reborn.npc.soul.GoalGenerator(plugin);
        this.goalProgressor = new kr.reborn.npc.soul.GoalProgressor(plugin);
        this.gossip = new kr.reborn.npc.social.GossipManager(plugin);
        var s = plugin.getConfig().getConfigurationSection("emotion-decay-rate");
        for (Emotion.Kind k : Emotion.Kind.values()) {
            decayRates.put(k, s == null ? 0.5 : s.getDouble(k.name().toLowerCase(), 0.5));
        }
    }

    public RebornNpc get(String id) { return byId.get(id); }
    public java.util.Collection<RebornNpc> all() { return byId.values(); }

    public RebornNpc spawn(String id, String name, WorldKey world, Location loc, String faction, String job) {
        RebornNpc n = new RebornNpc(id, name, world, loc);
        n.faction = faction;
        n.job = job;
        // 직업 기반 성격 부여 (자녀 NPC는 ChildbirthBehavior에서 부모 평균 성격으로 덮어씀)
        n.soul = new kr.reborn.npc.soul.Soul(kr.reborn.npc.soul.Personality.fromJob(job));
        byId.put(id, n);
        materialize(n);
        return n;
    }

    public void remove(String id) {
        RebornNpc n = byId.remove(id);
        if (n != null && n.bukkitEntityId != null) {
            var e = Bukkit.getEntity(n.bukkitEntityId);
            if (e != null) e.remove();
        }
    }

    private void materialize(RebornNpc n) {
        if (n.location == null) return;
        World w = n.location.getWorld();
        if (w == null) return;
        var v = w.spawn(n.location, n.defaultEntity());
        v.setCustomName(n.displayName);
        v.setCustomNameVisible(true);
        v.setAI(true);  // 진짜 AI 켬 — Pathfinder 활용
        v.setRemoveWhenFarAway(false);  // 청크 언로드되도 유지
        v.setInvulnerable(false);
        // HP·공격력 stat 적용
        try {
            double maxHp = Math.max(20, n.stats.getOrDefault("ENDURANCE", 20.0) * 2);
            var attr = v.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
            if (attr != null) { attr.setBaseValue(maxHp); v.setHealth(maxHp); }
            var dmg = v.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE);
            if (dmg != null) dmg.setBaseValue(Math.max(1, n.stats.getOrDefault("STRENGTH", 5.0)));
        } catch (Throwable ignored) {}
        n.bukkitEntityId = v.getUniqueId();
        attachBrain(n);
    }

    /** RebornNpc에 NpcBrain을 등록하고 표준 Behavior 9종 부착. */
    public void attachBrain(RebornNpc n) {
        if (n.brain != null) return;
        NpcBrain brain = new NpcBrain(n);
        brain.register(new IdleBehavior());
        brain.register(new ScheduleBehavior());
        brain.register(new PatrolBehavior());
        brain.register(new CombatBehavior(plugin));
        brain.register(new FleeBehavior());
        brain.register(new SocialBehavior(plugin));
        brain.register(new RevengeBehavior(plugin));
        brain.register(new ChildbirthBehavior(plugin));
        brain.register(new kr.reborn.npc.ai.behavior.PursueGoalBehavior(plugin));
        n.brain = brain;
    }

    public kr.reborn.npc.soul.GoalGenerator goalGenerator() { return goalGenerator; }
    public kr.reborn.npc.soul.GoalProgressor goalProgressor() { return goalProgressor; }
    public kr.reborn.npc.social.SocialNetwork socialNetwork() { return socialNetwork; }
    public kr.reborn.npc.social.GossipManager gossip() { return gossip; }
    private int gossipTickCounter = 0;

    public void tickAll() {
        for (RebornNpc n : byId.values()) {
            if (n.dead) continue;
            n.emotion.decay(decayRates);
            // 영혼 — 욕구 자연 감쇠, 가상 나이 누적
            if (n.soul != null) {
                n.soul.needs.decay();
                n.soul.ageYears += 1.0 / (60.0 * 60.0 * 24.0 * 365.0)
                        * (plugin.getConfig().getLong("ai-tick-interval", 10L) * 50.0 / 1000.0);
            }
            // 목표 — 자연 진행 + 새 목표 검토
            goalProgressor.tick(n);
            goalGenerator.considerNewGoal(n);
            // 완료된 목표는 archive로 이동
            n.goals.removeIf(g -> {
                if (g.isFulfilled() || g.abandoned) {
                    n.goalsArchive.add(g);
                    return true;
                }
                return false;
            });
            // entity가 살아있나 확인
            if (n.bukkitEntityId != null) {
                var ent = Bukkit.getEntity(n.bukkitEntityId);
                if (ent == null || ent.isDead()) {
                    if (ent != null && ent.isDead()) {
                        n.dead = true;
                        n.deathAt = System.currentTimeMillis();
                        triggerRevengeForFriends(n);
                    }
                    continue;
                }
                if (ent instanceof Mob mob) {
                    n.location = mob.getLocation();
                }
            }
            // 소문 평판 감쇠 (서서히 잊혀짐)
            if (n.soul != null) n.soul.reputation.decay();

            if (n.brain != null) n.brain.tick();
            else SimpleAI.step(n);
        }
        // 소문 전파 — 5사이클마다 1번 (성능)
        if (++gossipTickCounter >= 5) {
            gossipTickCounter = 0;
            gossip.propagate();
        }
    }

    /** NPC 사망 시 친한 NPC들에게 복수 트리거 등록. */
    private void triggerRevengeForFriends(RebornNpc dead) {
        if (dead.killerId == null) return;
        for (RebornNpc other : byId.values()) {
            if (other.dead || other == dead) continue;
            if (other.relations.npc(dead.id) >= 50) {
                other.aiData.put("revenge:target", dead.killerId);
                other.aiData.put("revenge:until", System.currentTimeMillis() + 3_600_000L); // 1시간
                other.emotion.add(Emotion.Kind.ANGER, 50);
                other.emotion.add(Emotion.Kind.SADNESS, 30);
            }
        }
    }

    public void loadAll() {
        // 0) 플러그인 jar 내장 기본 좌표 (npcs-lobby.yml) 추출
        plugin.saveResource("npcs-lobby.yml", false);

        // 1) config.yml의 npcs: 섹션을 사전 정의 NPC로 등록 (좌표는 임시, 운영 시 /rnpc spawn으로 배치)
        var npcSec = plugin.getConfig().getConfigurationSection("npcs");
        if (npcSec != null) {
            for (String id : npcSec.getKeys(false)) {
                if (byId.containsKey(id)) continue;
                var s = npcSec.getConfigurationSection(id);
                if (s == null) continue;
                String name = s.getString("name", id);
                WorldKey world;
                try { world = WorldKey.valueOf(s.getString("world", "LOBBY")); }
                catch (Exception e) { world = WorldKey.LOBBY; }
                String faction = s.getString("faction", "");
                String job = s.getString("job", "VILLAGER");
                RebornNpc n = new RebornNpc(id, name, world, null);
                n.faction = faction;
                n.job = job;
                n.hermit = s.getBoolean("hermit", false);
                var statSec = s.getConfigurationSection("stats");
                if (statSec != null) {
                    for (String k : statSec.getKeys(false)) n.stats.put(k, statSec.getDouble(k));
                }
                byId.put(id, n);
            }
            plugin.getLogger().info("config 사전 정의 NPC " + byId.size() + "개 등록");
        }

        // 2) 좌표 포함 NPC 파일들 로드 (npcs-lobby.yml, npcs.yml)
        loadCoordsFromFile(new File(plugin.getDataFolder(), "npcs-lobby.yml"));
        loadCoordsFromFile(new File(plugin.getDataFolder(), "npcs.yml"));
    }

    private void loadCoordsFromFile(File f) {
        if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        for (String id : y.getKeys(false)) {
            String name = y.getString(id + ".name", id);
            WorldKey world;
            try { world = WorldKey.valueOf(y.getString(id + ".world", "FANTASY")); }
            catch (Exception e) { world = WorldKey.FANTASY; }
            String worldName = y.getString(id + ".bukkit-world", "world");
            World bw = Bukkit.getWorld(worldName);
            if (bw == null) continue;
            Location loc = new Location(bw,
                    y.getDouble(id + ".x"), y.getDouble(id + ".y"), y.getDouble(id + ".z"));
            RebornNpc target;
            RebornNpc existing = byId.get(id);
            if (existing != null) {
                existing.location = loc;
                materialize(existing);
                target = existing;
            } else {
                target = spawn(id, name, world, loc,
                        y.getString(id + ".faction", ""), y.getString(id + ".job", "VILLAGER"));
                target.hermit = y.getBoolean(id + ".hermit", false);
                var stSec = y.getConfigurationSection(id + ".stats");
                if (stSec != null) {
                    for (String key : stSec.getKeys(false)) target.stats.put(key, stSec.getDouble(key));
                }
            }
            // home, workplace 선택적 좌표
            if (y.contains(id + ".home")) {
                target.home = new Location(bw,
                        y.getDouble(id + ".home.x"), y.getDouble(id + ".home.y"), y.getDouble(id + ".home.z"));
            }
            if (y.contains(id + ".workplace")) {
                target.workplace = new Location(bw,
                        y.getDouble(id + ".workplace.x"), y.getDouble(id + ".workplace.y"), y.getDouble(id + ".workplace.z"));
            }
            // 신·여신 등은 일과·전투 비활성화 (브레인 비우기)
            String job = target.job;
            if ("GODDESS".equals(job) || "GOD".equals(job)
                    || "DEMON_LORD".equals(job) || "ARCHANGEL".equals(job)
                    || "SPIRIT_KING".equals(job) || "PRIMORDIAL".equals(job)) {
                target.brain = null;
                var ent = target.bukkitEntityId == null ? null : Bukkit.getEntity(target.bukkitEntityId);
                if (ent instanceof Mob mob) mob.setAI(false);
            }
            // 결혼·자녀·사망 복원
            target.spouseNpcId = y.getString(id + ".spouse", "");
            var children = y.getStringList(id + ".children");
            if (!children.isEmpty()) target.children.addAll(children);
            target.dead = y.getBoolean(id + ".dead", false);
        }
    }

    public void saveAll() {
        File f = new File(plugin.getDataFolder(), "npcs.yml");
        plugin.getDataFolder().mkdirs();
        YamlConfiguration y = new YamlConfiguration();
        for (RebornNpc n : byId.values()) {
            if (n.location == null) continue;  // 좌표 없는 사전 정의는 저장 안 함
            String b = n.id + ".";
            y.set(b + "name", n.displayName);
            y.set(b + "world", n.world.name());
            y.set(b + "bukkit-world", n.location.getWorld() == null ? "world" : n.location.getWorld().getName());
            y.set(b + "x", n.location.getX());
            y.set(b + "y", n.location.getY());
            y.set(b + "z", n.location.getZ());
            y.set(b + "faction", n.faction);
            y.set(b + "job", n.job);
            y.set(b + "hermit", n.hermit);
            if (n.home != null) {
                y.set(b + "home.x", n.home.getX());
                y.set(b + "home.y", n.home.getY());
                y.set(b + "home.z", n.home.getZ());
            }
            if (n.workplace != null) {
                y.set(b + "workplace.x", n.workplace.getX());
                y.set(b + "workplace.y", n.workplace.getY());
                y.set(b + "workplace.z", n.workplace.getZ());
            }
            if (!n.spouseNpcId.isEmpty()) y.set(b + "spouse", n.spouseNpcId);
            if (!n.children.isEmpty()) y.set(b + "children", n.children);
            y.set(b + "dead", n.dead);
            for (var e : n.stats.entrySet()) y.set(b + "stats." + e.getKey(), e.getValue());
        }
        try { y.save(f); } catch (Exception ignored) {}
    }

    public RebornNpc nearest(Location loc, double radius) {
        RebornNpc best = null; double dist = radius * radius;
        for (RebornNpc n : byId.values()) {
            if (n.location.getWorld() != loc.getWorld()) continue;
            double d = n.location.distanceSquared(loc);
            if (d < dist) { dist = d; best = n; }
        }
        return best;
    }

    public RebornNpc byEntity(UUID entityId) {
        for (RebornNpc n : byId.values()) {
            if (entityId.equals(n.bukkitEntityId)) return n;
        }
        return null;
    }
}
