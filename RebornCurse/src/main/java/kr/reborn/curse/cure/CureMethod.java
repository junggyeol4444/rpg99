package kr.reborn.curse.cure;

/**
 * 저주 해제 방법 enum — config의 cure_methods 문자열과 매칭.
 *
 * 각 메서드는 실제 트리거 방식이 다름:
 * - ITEM: PlayerInteractEvent의 손에 든 아이템으로
 * - NPC: NPC와의 interaction
 * - QUEST: RebornQuestCompleteEvent
 * - LOCATION: 특정 위치(또는 블록 종류) 도달
 * - SKILL: 스킬 캐스트 (ex. GREATER_SAGE_DISPEL)
 * - MECHANIC: 별도 매커니즘 (REMOVE_CYBERNETICS_3 등)
 * - AUTO_EXPIRE: 시간 만료 — 별도 처리
 */
public enum CureMethod {

    // ─── 아이템 (인벤토리 소비) ───
    BING_SIM_DAN          (Trigger.ITEM,     "minecraft:enchanted_golden_apple"),
    HAEDOK_PIL            (Trigger.ITEM,     "minecraft:potion"),
    JAGUEM_PIL            (Trigger.ITEM,     "minecraft:honey_bottle"),
    GUYANG_PILL           (Trigger.ITEM,     "minecraft:golden_apple"),
    ANTI_RAD_SERUM        (Trigger.ITEM,     "minecraft:milk_bucket"),
    NEURAL_STABILIZER     (Trigger.ITEM,     "minecraft:ender_pearl"),
    PURIFY_ORB            (Trigger.ITEM,     "minecraft:ender_eye"),
    FOOD                  (Trigger.ITEM,     "minecraft:cooked_beef"),

    // ─── NPC 상호작용 ───
    SOLIM_ABBOT_NPC       (Trigger.NPC,      "solim_abbot"),
    THERAPIST_NPC         (Trigger.NPC,      "cyber_therapist"),
    SEA_WITCH_NPC         (Trigger.NPC,      "sea_witch"),
    TAOIST_NPC            (Trigger.NPC,      "wandering_taoist"),
    CASTER_RELEASE        (Trigger.NPC,      "*"),  // 시전자 본인 (특수)

    // ─── 퀘스트 완료 ───
    HEAL_QUEST            (Trigger.QUEST,    "heal_qi_deviation"),
    ARCHANGEL_REPENT_QUEST(Trigger.QUEST,    "archangel_repent"),
    TAEHEO_REPENT_QUEST   (Trigger.QUEST,    "taeheo_repent"),
    DRAGON_APOLOGY_QUEST  (Trigger.QUEST,    "dragon_apology"),

    // ─── 위치 (특수 블록·구조물 도달) ───
    HARMONY_FOUNTAIN      (Trigger.LOCATION, "harmony_fountain"),
    MED_BAY               (Trigger.LOCATION, "med_bay"),
    LIGHT_RITUAL          (Trigger.LOCATION, "light_altar"),

    // ─── 스킬 시전 ───
    GREATER_SAGE_DISPEL   (Trigger.SKILL,    "greater_sage_dispel"),

    // ─── 별도 메커니즘 ───
    REMOVE_CYBERNETICS_3  (Trigger.MECHANIC, "cybernetics_count_max_3"),
    AUTO_EXPIRE           (Trigger.MECHANIC, "auto");

    public enum Trigger { ITEM, NPC, QUEST, LOCATION, SKILL, MECHANIC }

    public final Trigger trigger;
    public final String target;

    CureMethod(Trigger trigger, String target) {
        this.trigger = trigger;
        this.target = target;
    }

    public static CureMethod find(String id) {
        if (id == null) return null;
        try { return CureMethod.valueOf(id.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
}
