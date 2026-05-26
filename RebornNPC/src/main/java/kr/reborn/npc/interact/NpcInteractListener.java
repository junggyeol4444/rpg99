package kr.reborn.npc.interact;

import kr.reborn.core.event.RebornNPCInteractEvent;
import kr.reborn.npc.RebornNPC;
import kr.reborn.npc.emotion.Emotion;
import kr.reborn.npc.entity.RebornNpc;
import kr.reborn.npc.soul.Memory;
import kr.reborn.npc.soul.Needs;
import kr.reborn.npc.soul.Personality;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public final class NpcInteractListener implements Listener {

    private final RebornNPC plugin;

    public NpcInteractListener(RebornNPC plugin) { this.plugin = plugin; }

    @EventHandler
    public void onClick(PlayerInteractEntityEvent e) {
        var npc = plugin.registry().byEntity(e.getRightClicked().getUniqueId());
        if (npc == null) return;
        e.setCancelled(true);
        Bukkit.getPluginManager().callEvent(new RebornNPCInteractEvent(e.getPlayer(), npc.id));

        // 호감도 + 호기심
        npc.relations.addPlayer(e.getPlayer().getUniqueId(), 0.5);
        npc.emotion.add(Emotion.Kind.CURIOSITY, 1.0);

        // 영혼이 있으면 욕구·기억 갱신
        if (npc.soul != null) {
            npc.soul.needs.add(Needs.Kind.COMPANIONSHIP, +2);
            // 선물 — 손에 든 아이템 있으면 기록
            var item = e.getPlayer().getInventory().getItemInMainHand();
            if (item != null && !item.getType().isAir()) {
                npc.soul.memory.record(e.getPlayer().getUniqueId().toString(),
                        Memory.Kind.GIFTED_ME, 15, item.getType().name());
            }
            // 매번 만나면 약한 우호 기억
            npc.soul.memory.record(e.getPlayer().getUniqueId().toString(),
                    Memory.Kind.HELPED_ME, 2, "대화");
        }

        // 성격에 따른 인사말
        String greeting = "무슨 일이오?";
        if (npc.soul != null) {
            int empathy = npc.soul.personality.get(Personality.Trait.EMPATHY);
            int pride = npc.soul.personality.get(Personality.Trait.PRIDE);
            int soc = npc.soul.personality.get(Personality.Trait.SOCIABILITY);
            double sent = npc.soul.relationToward(e.getPlayer().getUniqueId().toString());
            if (sent > 60) greeting = "오, 자네 왔는가! 늘 반갑네.";
            else if (sent < -40) greeting = "...왜 또 왔나.";
            else if (pride > 50) greeting = "감히 나에게 말을 거는가?";
            else if (empathy > 50) greeting = "오, 반갑네. 무엇이 필요한가?";
            else if (soc < -30) greeting = "...";
        }
        e.getPlayer().sendMessage("§6[" + npc.displayName + "] §f" + greeting);

        // 환생의 여신 클릭 시 룰렛 발동
        if ("reincarnation_goddess".equals(npc.id)) {
            try {
                var spawnPlugin = Bukkit.getPluginManager().getPlugin("RebornSpawn");
                if (spawnPlugin != null) {
                    Object roulette = spawnPlugin.getClass().getMethod("roulette").invoke(spawnPlugin);
                    roulette.getClass().getMethod("spin", org.bukkit.entity.Player.class)
                            .invoke(roulette, e.getPlayer());
                }
            } catch (Throwable ignored) {}
        }

        // 은둔고수 정체 공개 체크
        if (npc.hermit && !npc.revealed) {
            int reveal = plugin.getConfig().getInt("hermit.reveal-favor", 80);
            if (npc.relations.player(e.getPlayer().getUniqueId()) >= reveal) {
                npc.revealed = true;
                Bukkit.broadcastMessage("§5[은둔고수] §f" + npc.displayName + "의 정체가 드러났다!");
            }
        }
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent e) {
        var npc = plugin.registry().byEntity(e.getEntity().getUniqueId());
        if (npc == null) return;
        npc.emotion.add(Emotion.Kind.ANGER, 25);
        npc.emotion.add(Emotion.Kind.TRUST, -10);
        npc.emotion.add(Emotion.Kind.FEAR, 15);
        if (e.getDamager() instanceof org.bukkit.entity.Player p) {
            npc.relations.addPlayer(p.getUniqueId(), -5);
            // 영혼 — 공격 기억 기록
            if (npc.soul != null) {
                int intensity = (int) Math.min(50, e.getFinalDamage() * 3);
                npc.soul.memory.record(p.getUniqueId().toString(),
                        Memory.Kind.ATTACKED_ME, intensity, "피격");
                npc.soul.reclassify(p.getUniqueId().toString());
                // 안전 욕구 감소
                npc.soul.needs.add(Needs.Kind.SAFETY, -10);
            }
            if (npc.relations.player(p.getUniqueId()) < -50) {
                npc.aiData.put("revenge:target", p.getUniqueId());
                npc.aiData.put("revenge:until", System.currentTimeMillis() + 1_800_000L);
            }
            // 큰 피해는 소문 — "X가 나를 공격했다더라"
            if (e.getFinalDamage() >= 8 && npc.soul != null) {
                plugin.registry().gossip().createRumor(
                        npc, p.getUniqueId().toString(), npc.id,
                        kr.reborn.npc.social.RumorContent.ATTACKED_BY,
                        (int) Math.min(60, e.getFinalDamage() * 2));
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        var npc = plugin.registry().byEntity(e.getEntity().getUniqueId());
        if (npc == null) return;
        npc.dead = true;
        npc.deathAt = System.currentTimeMillis();
        if (e.getEntity().getKiller() != null) {
            npc.killerId = e.getEntity().getKiller().getUniqueId();
            String killerStr = npc.killerId.toString();
            // 살해자에게 KILLED_TARGET 이벤트 발생 (자기 목표 AVENGE/DEFEAT_RIVAL 완료)
            var killerNpc = plugin.registry().byEntity(npc.killerId);
            if (killerNpc != null) {
                plugin.registry().goalProgressor().onEvent(killerNpc,
                        new kr.reborn.npc.soul.GoalProgressor.Event(
                                kr.reborn.npc.soul.GoalProgressor.EventKind.KILLED_TARGET, npc.id));
            }
            for (RebornNpc other : plugin.registry().all()) {
                if (other == npc || other.dead || other.soul == null) continue;
                double rel = other.soul.relationToward(npc.id);
                if (rel >= 70) {
                    other.soul.memory.record(killerStr, Memory.Kind.KILLED_MY_FAMILY, 100, "가족 살해");
                    other.emotion.add(Emotion.Kind.ANGER, 60);
                    other.emotion.add(Emotion.Kind.SADNESS, 50);
                    // PROTECT_FAMILY 목표 실패
                    plugin.registry().goalProgressor().onEvent(other,
                            new kr.reborn.npc.soul.GoalProgressor.Event(
                                    kr.reborn.npc.soul.GoalProgressor.EventKind.FAMILY_LOST, npc.id));
                } else if (rel >= 40) {
                    other.soul.memory.record(killerStr, Memory.Kind.KILLED_MY_FRIEND, 80, "친구 살해");
                    other.emotion.add(Emotion.Kind.ANGER, 40);
                    other.emotion.add(Emotion.Kind.SADNESS, 30);
                }
                // 주군 사망 → SERVE_LORD 무효
                plugin.registry().goalProgressor().onEvent(other,
                        new kr.reborn.npc.soul.GoalProgressor.Event(
                                kr.reborn.npc.soul.GoalProgressor.EventKind.LORD_DIED, npc.id));
            }
            // 소문 생성 — "살해자가 사람을 죽였다더라" (목격 NPC가 있으면)
            RebornNpc nearestWitness = plugin.registry().nearest(npc.location, 20);
            if (nearestWitness != null && !nearestWitness.dead) {
                plugin.registry().gossip().createRumor(
                        nearestWitness, killerStr, npc.id,
                        kr.reborn.npc.social.RumorContent.MURDERED, 90);
            }
        }
        // 사회망에서 제거
        plugin.registry().socialNetwork().removeAllOf(npc.id);
        Bukkit.broadcastMessage("§7§o[NPC 사망] §r" + npc.displayName + "이(가) 쓰러졌다.");
    }
}
