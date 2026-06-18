package kr.reborn.curse.cure;

import kr.reborn.core.util.Msg;
import kr.reborn.curse.RebornCurse;
import kr.reborn.curse.data.ActiveEffect;
import kr.reborn.curse.data.EffectDef;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 저주 해제 엔진.
 *
 * 각 ActiveEffect의 cure_methods 리스트 중 하나라도 충족되면 해제.
 * 트리거 타입별로 다른 방식:
 *   - ITEM: 인벤토리 소비 + 1 (PlayerInteractEvent에서 손에 든 아이템 검사)
 *   - NPC: NPC 상호작용 (RebornNPCInteractEvent)
 *   - QUEST: 퀘스트 완료 (RebornQuestCompleteEvent)
 *   - LOCATION: 특정 블록 종류 위에 서있을 때 (LOCATION 검사)
 *   - SKILL: 누가 스킬 시전 (RebornSkill cast event)
 *   - MECHANIC: REMOVE_CYBERNETICS_3 등 별도 처리
 */
public final class CureEngine {

    private final RebornCurse plugin;
    /** 위치 기반 cure 적용 중복 방지 */
    private final Set<UUID> recentLocationCured = new HashSet<>();

    public CureEngine(RebornCurse plugin) {
        this.plugin = plugin;
    }

    /** 플레이어가 ID에 해당하는 효과를 들고 있을 때, 어떤 cure_method가 적용 가능한지. */
    public boolean tryCureWithItem(Player p, ItemStack hand) {
        if (hand == null || hand.getType() == Material.AIR) return false;
        String materialKey = "minecraft:" + hand.getType().name().toLowerCase();
        boolean curedAny = false;
        for (var e : new java.util.HashMap<>(plugin.effects().of(p.getUniqueId())).entrySet()) {
            ActiveEffect a = e.getValue();
            EffectDef def = plugin.registry().get(a.id);
            if (def == null || def.kind != EffectDef.Kind.CURSE) continue;
            for (String method : def.cureMethods) {
                CureMethod cm = CureMethod.find(method);
                if (cm == null || cm.trigger != CureMethod.Trigger.ITEM) continue;
                if (!cm.target.equals(materialKey)) continue;
                // 아이템 소비
                hand.setAmount(hand.getAmount() - 1);
                plugin.effects().cure(p, a.id);
                Msg.send(p, "&a&l[해제] &f" + def.name + " &7가 " + cm.name() + " 사용으로 풀렸다.");
                curedAny = true;
                break;
            }
            if (curedAny) break;
        }
        return curedAny;
    }

    public boolean tryCureWithNpc(Player p, String npcId) {
        if (npcId == null) return false;
        boolean cured = false;
        for (var e : new java.util.HashMap<>(plugin.effects().of(p.getUniqueId())).entrySet()) {
            ActiveEffect a = e.getValue();
            EffectDef def = plugin.registry().get(a.id);
            if (def == null || def.kind != EffectDef.Kind.CURSE) continue;
            for (String method : def.cureMethods) {
                CureMethod cm = CureMethod.find(method);
                if (cm == null || cm.trigger != CureMethod.Trigger.NPC) continue;
                if (!cm.target.equals(npcId) && !"*".equals(cm.target)) continue;
                plugin.effects().cure(p, a.id);
                Msg.send(p, "&a&l[해제] &f" + def.name + " &7가 " + npcId + " NPC에 의해 풀렸다.");
                cured = true;
                break;
            }
        }
        return cured;
    }

    public boolean tryCureWithQuest(Player p, String questId) {
        if (questId == null) return false;
        boolean cured = false;
        for (var e : new java.util.HashMap<>(plugin.effects().of(p.getUniqueId())).entrySet()) {
            ActiveEffect a = e.getValue();
            EffectDef def = plugin.registry().get(a.id);
            if (def == null || def.kind != EffectDef.Kind.CURSE) continue;
            for (String method : def.cureMethods) {
                CureMethod cm = CureMethod.find(method);
                if (cm == null || cm.trigger != CureMethod.Trigger.QUEST) continue;
                if (!cm.target.equals(questId)) continue;
                plugin.effects().cure(p, a.id);
                Msg.send(p, "&a&l[해제] &f" + def.name + " &7가 퀘스트 완료로 풀렸다.");
                cured = true;
                break;
            }
        }
        return cured;
    }

    public boolean tryCureAtLocation(Player p, String locationTag) {
        UUID id = p.getUniqueId();
        // 중복 방지: 같은 위치 cure는 30초 이내 1회
        if (recentLocationCured.contains(id)) return false;
        boolean cured = false;
        for (var e : new java.util.HashMap<>(plugin.effects().of(id)).entrySet()) {
            ActiveEffect a = e.getValue();
            EffectDef def = plugin.registry().get(a.id);
            if (def == null || def.kind != EffectDef.Kind.CURSE) continue;
            for (String method : def.cureMethods) {
                CureMethod cm = CureMethod.find(method);
                if (cm == null || cm.trigger != CureMethod.Trigger.LOCATION) continue;
                if (!cm.target.equals(locationTag)) continue;
                plugin.effects().cure(p, a.id);
                Msg.send(p, "&a&l[해제] &f" + def.name + " &7가 " + locationTag + "에서 정화되었다.");
                cured = true;
                recentLocationCured.add(id);
                // 30초 후 해제
                kr.reborn.core.RebornCore.get().scheduler().runTaskLater(
                        () -> recentLocationCured.remove(id), 600L);
                break;
            }
        }
        return cured;
    }

    public boolean tryCureWithSkill(Player p, String skillId) {
        boolean cured = false;
        for (var e : new java.util.HashMap<>(plugin.effects().of(p.getUniqueId())).entrySet()) {
            ActiveEffect a = e.getValue();
            EffectDef def = plugin.registry().get(a.id);
            if (def == null || def.kind != EffectDef.Kind.CURSE) continue;
            for (String method : def.cureMethods) {
                CureMethod cm = CureMethod.find(method);
                if (cm == null || cm.trigger != CureMethod.Trigger.SKILL) continue;
                if (!cm.target.equals(skillId)) continue;
                plugin.effects().cure(p, a.id);
                Msg.send(p, "&a&l[해제] &f" + def.name + " &7가 " + skillId + " 스킬로 풀렸다.");
                cured = true;
                break;
            }
        }
        return cured;
    }

    /** REMOVE_CYBERNETICS_3 등 매커니즘 트리거 — 외부에서 호출. */
    public boolean tryCureMechanic(Player p, String mechanic) {
        boolean cured = false;
        for (var e : new java.util.HashMap<>(plugin.effects().of(p.getUniqueId())).entrySet()) {
            ActiveEffect a = e.getValue();
            EffectDef def = plugin.registry().get(a.id);
            if (def == null || def.kind != EffectDef.Kind.CURSE) continue;
            for (String method : def.cureMethods) {
                CureMethod cm = CureMethod.find(method);
                if (cm == null || cm.trigger != CureMethod.Trigger.MECHANIC) continue;
                if (!cm.target.equals(mechanic)) continue;
                plugin.effects().cure(p, a.id);
                Msg.send(p, "&a&l[해제] &f" + def.name + " &7가 " + mechanic + "로 풀렸다.");
                cured = true;
                break;
            }
        }
        return cured;
    }
}
