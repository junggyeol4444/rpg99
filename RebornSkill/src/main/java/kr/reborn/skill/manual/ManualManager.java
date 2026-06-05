package kr.reborn.skill.manual;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.core.util.Rand;
import kr.reborn.skill.RebornSkill;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 비급 매니저.
 *
 * 시드 30종 비급:
 *   무협 (10): 독고구검·항룡십팔장·구양진경·태극권·만류귀종 등
 *   판타지 (6): 일루션·메테오·텔레포트·헤이스트·소생·블링크
 *   마계 (4): 혈마공·뇌마수·심마결·살수공
 *   천계 (3): 천신경·구원의빛·정화
 *   기타 (7): 정령왕소환·드래곤브레스·해룡진·사이버해킹·요왕변신·아포생존·시간조작
 *
 * 진행:
 *   1. discover(p, manualId) - 발견 (퀘스트·드롭·도난 트리거)
 *   2. startResearch(p, manualId) - 연구 시작 (시간 필요)
 *   3. complete() (자동) - 시간 지나면 스킬 학습
 *
 * 도난:
 *   stealFromNpc(p, npcId) - 5% 확률, charisma 가산
 *   실패 시 NPC 호의 -50
 *
 * 시장 거래:
 *   transferTo(seller, buyer, manualId, price) - 비급 양도
 */
public final class ManualManager {

    private final RebornSkill plugin;
    private final Map<String, SecretManual> manuals = new HashMap<>();
    /** uuid → 보유한 비급 set */
    private final Map<UUID, java.util.Set<String>> owned = new ConcurrentHashMap<>();
    /** uuid → manualId → 연구 시작 시각 (ms). 0 = 미연구. */
    private final Map<UUID, Map<String, Long>> research = new ConcurrentHashMap<>();
    /** NPC 비급 인벤토리 (npcId → manualIds) */
    private final Map<String, java.util.Set<String>> npcInventory = new HashMap<>();

    public ManualManager(RebornSkill plugin) {
        this.plugin = plugin;
        seedManuals();
        seedNpcInventories();
        // 1분마다 연구 완료 체크
        RebornCore.get().scheduler().runTimer(this::tickResearch, 1200L, 1200L);
    }

    private void seedManuals() {
        // 무협 비급
        add("dokgo_guzeom", "독고구검", "dokgo_gugeom", SecretManual.Rarity.LEGENDARY,
                WorldKey.MARTIAL, 60, "hidden_dokgo_tomb", "독행대협이 남긴 검결");
        add("hangryong_18", "항룡십팔장", "hangryong", SecretManual.Rarity.EPIC,
                WorldKey.MARTIAL, 45, "alliance_secret_vault", "강룡 18장의 신비");
        add("guyang_jin", "구양진경", "guyang_jin", SecretManual.Rarity.LEGENDARY,
                WorldKey.MARTIAL, 50, "shaolin_inner_chamber", "양강의 극의");
        add("taegeuk_kwon", "태극권", "taegeuk", SecretManual.Rarity.RARE,
                WorldKey.MARTIAL, 30, "wudang_temple", "음양 조화의 무공");
        add("manryu_gwijong", "만류귀종", "manryu", SecretManual.Rarity.MYTHIC,
                WorldKey.MARTIAL, 180, "ancient_master_cave", "만 가지 무공이 하나로");
        add("bing_jam_dok_gyung", "빙잠독경", "bingjam_dokgyeong", SecretManual.Rarity.EPIC,
                WorldKey.MARTIAL, 40, "ice_silkworm_lair", "만년 빙잠의 독공");
        add("chilsang_gwon", "칠상권", "chilsang_gwon", SecretManual.Rarity.RARE,
                WorldKey.MARTIAL, 25, "iron_bear_cave", "철갑웅의 권법");
        add("hyeolma_gong", "혈마공", "hyeolma_gong", SecretManual.Rarity.LEGENDARY,
                WorldKey.MARTIAL, 90, "demon_cult_secret_chamber", "마교의 금단 무공");
        add("sura_magong", "수라마공", "sura_magong", SecretManual.Rarity.EPIC,
                WorldKey.MARTIAL, 60, "sura_clan_archive", "수라도의 마공");
        add("samje_basic_form", "삼재검법", "samje_basic_form", SecretManual.Rarity.COMMON,
                WorldKey.MARTIAL, 5, "wandering_master", "무인의 기초");

        // 판타지
        add("illusion_legendary", "환영 마스터의 책", "illusion_master",
                SecretManual.Rarity.LEGENDARY, WorldKey.FANTASY, 60,
                "mage_tower_library", "환영 마법의 정수");
        add("meteor_grimoire", "유성 그리모어", "book_meteor",
                SecretManual.Rarity.LEGENDARY, WorldKey.FANTASY, 70,
                "dragon_red_hoard", "하늘에서 별을 떨군다");
        add("teleport_tome", "텔레포트 결",  "blink", SecretManual.Rarity.RARE,
                WorldKey.FANTASY, 20, "elf_archive", "순간이동");
        add("haste_book", "신속의 서", "haste", SecretManual.Rarity.COMMON,
                WorldKey.FANTASY, 10, "any_library", "기본 마법");
        add("resurrection_legendary", "부활의 서", "book_resurrection",
                SecretManual.Rarity.MYTHIC, WorldKey.FANTASY, 180,
                "celestial_court", "신만이 알던 비기");
        add("arteon_blade", "아르테온의 검", "book_arteon_blade",
                SecretManual.Rarity.EPIC, WorldKey.FANTASY, 35,
                "knight_order_vault", "전설의 검술");

        // 마계
        add("hellfire_grimoire", "지옥불 그리모어", "demon_hellfire",
                SecretManual.Rarity.EPIC, WorldKey.DEMON, 50,
                "demon_lord_library", "지옥의 화염");
        add("soul_drain", "영혼 흡수", "soul_drain", SecretManual.Rarity.LEGENDARY,
                WorldKey.DEMON, 90, "lich_phylactery", "악마의 핵심");
        add("nightmare_curse", "악몽 저주", "nightmare", SecretManual.Rarity.EPIC,
                WorldKey.DEMON, 40, "succubus_chambers", "꿈을 망친다");
        add("demon_pact", "악마 계약서", "demon_pact", SecretManual.Rarity.LEGENDARY,
                WorldKey.DEMON, 60, "marwang_archive", "마왕과의 계약");

        // 천계
        add("celestial_judgment", "천계 심판", "judgment",
                SecretManual.Rarity.LEGENDARY, WorldKey.HEAVEN, 80,
                "celestial_throne", "신의 심판");
        add("holy_radiance", "성광의 비기", "holy_ray",
                SecretManual.Rarity.RARE, WorldKey.HEAVEN, 25,
                "temple_inner", "성스러운 빛");
        add("purification", "정화 의식", "purify_orb",
                SecretManual.Rarity.COMMON, WorldKey.HEAVEN, 15,
                "any_temple", "신성한 정화");

        // 정령계
        add("spirit_king_summon", "정령왕 소환", "spirit_king_summon",
                SecretManual.Rarity.MYTHIC, WorldKey.SPIRIT, 200,
                "primordial_grove", "정령왕 강림");

        // 드래곤
        add("dragon_breath_advanced", "용염결", "dragon_fire_breath",
                SecretManual.Rarity.EPIC, WorldKey.DRAGON, 60,
                "ancient_dragon_lair", "용의 입김");

        // 해양
        add("sea_dragon_array", "해룡진", "ocean_water_jet",
                SecretManual.Rarity.LEGENDARY, WorldKey.OCEAN, 80,
                "atlantis_palace", "바다의 용");

        // 사이버
        add("cyber_master_hack", "마스터 해킹 프로토콜", "cyber_master_hack",
                SecretManual.Rarity.LEGENDARY, WorldKey.CYBERPUNK, 90,
                "megacorp_alpha_core", "최상위 해킹");

        // 요계
        add("yokai_transformation", "구미호 변신술", "transform_human",
                SecretManual.Rarity.EPIC, WorldKey.YOKAI, 50,
                "kumiho_den", "9꼬리의 변신");

        // 아포
        add("apoc_survival_master", "폐허 생존 비기", "survival_kit",
                SecretManual.Rarity.RARE, WorldKey.APOCALYPSE, 30,
                "survivor_archive", "방사능 속 생존");

        // 시간
        add("time_manipulation", "시간 조작술", "time_rewind",
                SecretManual.Rarity.MYTHIC, WorldKey.MARTIAL, 240,
                "time_guardian_temple", "시간의 흐름을 거스른다");

        // ===== 기획서 5-5 추가 무협 비급 (40종 보강) =====
        add("cheonma_singong", "천마신공", "cheonma_singong",
                SecretManual.Rarity.MYTHIC, WorldKey.MARTIAL, 180,
                "cult_secret_chamber", "마교 최강 비급 — 천마의 경지에 이르는 유일한 무공");
        add("heupseung_daebeop", "흡성대법", "heupseung_daebeop",
                SecretManual.Rarity.LEGENDARY, WorldKey.MARTIAL, 90,
                "sapa_underground", "타인의 내공을 흡수하는 금기 무공");
        add("yeokgeun_segyeong", "역근세수경", "yeokgeun_segyeong",
                SecretManual.Rarity.LEGENDARY, WorldKey.MARTIAL, 120,
                "shaolin_inner_chamber", "근골을 재구성하여 신체 한계 돌파");
        add("bichun_singong", "비천신공", "bichun_singong",
                SecretManual.Rarity.LEGENDARY, WorldKey.MARTIAL, 100,
                "western_desert_ruins", "경공의 극치, 완성 시 자유 비행");
        add("bukmyeong_singong", "북명신공", "bukmyeong_singong",
                SecretManual.Rarity.MYTHIC, WorldKey.MARTIAL, 150,
                "north_sea_abyss", "북해 심연 내공 변환술");
        add("geongon_daenai", "건곤대나이", "geongon_daenai",
                SecretManual.Rarity.LEGENDARY, WorldKey.MARTIAL, 100,
                "mingo_secret_vault", "공격 흡수·반전의 7층 변환술");
        add("saja_hu", "사자후", "saja_hu",
                SecretManual.Rarity.EPIC, WorldKey.MARTIAL, 60,
                "shaolin_temple", "내공을 소리에 실어 광역 정신 공격");
        add("tanji_sintong", "탄지신통", "tanji_sintong",
                SecretManual.Rarity.RARE, WorldKey.MARTIAL, 40,
                "common_wuxia_market", "손가락 튕김으로 검기 발사");
        add("ognyeo_simgyeong", "옥녀심경", "ognyeo_simgyeong",
                SecretManual.Rarity.LEGENDARY, WorldKey.MARTIAL, 100,
                "ognyeo_palace", "쌍수 합벽 전용 — 단독 수련 시 주화입마");
        add("guyumchong_jingyeong", "구음진경", "guyum_jingyeong",
                SecretManual.Rarity.LEGENDARY, WorldKey.MARTIAL, 120,
                "wudang_secret_room", "도가 음기 비급 — 구양진경과 쌍벽");
        add("gyuhwa_botjeon", "규화보전", "gyuhwa_botjeon",
                SecretManual.Rarity.MYTHIC, WorldKey.MARTIAL, 180,
                "imperial_palace_secret", "금기 무공 — 천하무적급 속도·검술");
        add("taggu_bongbeop", "타구봉법", "taggu_bongbeop",
                SecretManual.Rarity.RARE, WorldKey.MARTIAL, 30,
                "gaebang_main_hall", "개방 방주에게만 전수되는 비전 봉법");
        add("ilyangji", "일양지", "ilyangji",
                SecretManual.Rarity.EPIC, WorldKey.MARTIAL, 60,
                "danli_palace", "손가락 끝에 양기를 집중한 원거리 점혈");
        add("hwasan_geombeop", "화산검법", "hwasan_geombeop",
                SecretManual.Rarity.RARE, WorldKey.MARTIAL, 40,
                "hwasan_temple", "기검(변칙)과 정검(정통) 두 갈래");
        add("byeoksa_geombeop", "벽사검법", "byeoksa_geombeop",
                SecretManual.Rarity.MYTHIC, WorldKey.MARTIAL, 180,
                "imperial_palace_secret", "규화보전 파생 — 천하 최고의 속도");
        add("yukmaeg_singeom", "육맥신검", "yukmaeg_singeom",
                SecretManual.Rarity.LEGENDARY, WorldKey.MARTIAL, 130,
                "dali_royal_chamber", "6개 손가락에서 검기 발사");
        add("neungpa_mibo", "능파미보", "neungpa_mibo",
                SecretManual.Rarity.EPIC, WorldKey.MARTIAL, 80,
                "ihyeong_hidden_cave", "역경 64괘 기반 신법 — 회피의 극치");
        add("dumi_jii", "두미지", "dumi_jii",
                SecretManual.Rarity.EPIC, WorldKey.MARTIAL, 70,
                "muyong_hidden_temple", "공격 흡수·반격의 전환 무공");
        add("manryu_gwijong", "만류귀종", "manryu",
                SecretManual.Rarity.MYTHIC, WorldKey.MARTIAL, 240,
                "deep_meditation_caves", "모든 무공 원리 통합 깨달음형");
        add("musang_singong", "무상신공", "musang_singong",
                SecretManual.Rarity.MYTHIC, WorldKey.MARTIAL, 200,
                "wuyong_ancient_vault", "형태가 없는 내공 — 어떤 무공이든 모방");
        add("muyeong_sinbeop", "무영신법", "muyeong_sinbeop",
                SecretManual.Rarity.EPIC, WorldKey.MARTIAL, 60,
                "assassin_guild_secret", "그림자조차 남기지 않는 은신 신법");
        add("hwagong_daebeop", "화공대법", "hwagong_daebeop",
                SecretManual.Rarity.LEGENDARY, WorldKey.MARTIAL, 90,
                "seongsupa_ancient", "상대 내공 완전 소멸 — 금기 무공");
        add("baekbo_singwon", "백보신권", "baekbo_singwon",
                SecretManual.Rarity.EPIC, WorldKey.MARTIAL, 80,
                "shaolin_temple", "권기를 100보 밖까지 날리는 원거리 권법");
        add("manggeom_gwijong", "만검귀종", "manggeom_gwijong",
                SecretManual.Rarity.MYTHIC, WorldKey.MARTIAL, 200,
                "geom_immortal_cave", "이기어검술의 궁극 — 수만 검 동시 조종");
        add("hwangol_taltae", "환골탈태술", "hwangol_taltae",
                SecretManual.Rarity.MYTHIC, WorldKey.MARTIAL, 200,
                "seongsupa_ancient", "신체 완전 재구성 — 성공 시 전 스탯 2배");
        add("cheonin_habil_gong", "천인합일공", "cheonin_habil_gong",
                SecretManual.Rarity.MYTHIC, WorldKey.MARTIAL, 240,
                "deep_meditation_caves", "천지와 합일하는 궁극의 내공심법");
        add("sibalban_muye", "십팔반무예", "sibalban_muye",
                SecretManual.Rarity.LEGENDARY, WorldKey.MARTIAL, 150,
                "muye_grandmaster_school", "18가지 무기 극한 수련 깨달음");
        add("polong_geombeop", "폭풍검법", "polong_geombeop",
                SecretManual.Rarity.RARE, WorldKey.MARTIAL, 40,
                "common_wuxia_market", "폭풍을 일으키는 단체전 특화 검법");
        add("sabaeg_pagoda", "철포삼", "cheolposam",
                SecretManual.Rarity.EPIC, WorldKey.MARTIAL, 60,
                "shaolin_temple", "전신 근육·피부 내공 강화 방어 무공");
        add("geumjong_jo", "금종조", "geumjong_jo",
                SecretManual.Rarity.LEGENDARY, WorldKey.MARTIAL, 100,
                "shaolin_inner_chamber", "소림 72절기 중 최강 방어 무공");

        // ===== 기획서 5-6 추가 선계 비급 (4종) =====
        add("cheonhwa_bongsin_gyeol", "천화봉신결", "cheonhwa_bongsin",
                SecretManual.Rarity.MYTHIC, WorldKey.IMMORTAL, 240,
                "taeheo_immortal_palace", "태허선궁 비전 — 신에 근접하는 힘");
        add("gujeon_hyeongong", "구전현공", "gujeon_hyeongong",
                SecretManual.Rarity.MYTHIC, WorldKey.IMMORTAL, 200,
                "ancient_caveheaven", "9번의 돌파로 선기 극한 정제");
        add("taeeul_noebeop", "태을뇌법", "taeeul_noebeop",
                SecretManual.Rarity.LEGENDARY, WorldKey.IMMORTAL, 120,
                "lightning_caveheaven", "뇌전 조종 도술 — 천사 경지 진가");
        add("hondonsimbeob", "혼돈심법", "hondonsimbeob",
                SecretManual.Rarity.LEGENDARY, WorldKey.IMMORTAL, 150,
                "honbang_marketplace", "선기+마기 동시 수련 금기 — 마선의 길");

        // ===== 기획서 5-1 추가 판타지 마법서 (6종 핵심) =====
        add("meteor_strike", "메테오 스트라이크", "meteor",
                SecretManual.Rarity.MYTHIC, WorldKey.FANTASY, 240,
                "magic_tower_top", "9서클 — 도시 하나를 소멸시키는 운석 소환");
        add("time_stop", "타임 스톱", "time_stop",
                SecretManual.Rarity.MYTHIC, WorldKey.FANTASY, 240,
                "ancient_archmage_vault", "9서클 — 시간 정지 (금기 마법)");
        add("dragon_word", "드래곤 워드", "dragon_word",
                SecretManual.Rarity.MYTHIC, WorldKey.FANTASY, 300,
                "dragon_lair_ancient", "용언 마법 — 드래곤으로부터 직접 전수");
        add("dimension_gate", "디멘션 게이트", "dimension_gate",
                SecretManual.Rarity.LEGENDARY, WorldKey.FANTASY, 150,
                "dimensional_research_lab", "8서클 — 차원 이동 마법");
        add("gate_of_babylon", "게이트 오브 바빌론", "gate_of_babylon",
                SecretManual.Rarity.MYTHIC, WorldKey.FANTASY, 240,
                "ancient_archmage_vault", "9서클 — 보관 차원 무기 동시 사출");
        add("life_drain", "라이프 드레인", "life_drain",
                SecretManual.Rarity.EPIC, WorldKey.FANTASY, 80,
                "necromancer_tower", "6서클 — 흑마법 분류, 생명력 흡수");
    }

    private void add(String id, String name, String skillId, SecretManual.Rarity r,
                     WorldKey w, int min, String at, String desc) {
        manuals.put(id, new SecretManual(id, name, skillId, r, w, min, at, desc));
    }

    private void seedNpcInventories() {
        // 일부 NPC가 처음부터 비급 보유 (도난 가능)
        addToNpc("alliance_master", "hangryong_18");
        addToNpc("cult_master", "hyeolma_gong");
        addToNpc("solim_abbot", "guyang_jin");
        addToNpc("wandering_taoist", "taegeuk_kwon");
        addToNpc("mage_tower_master", "illusion_legendary");
        addToNpc("dwarf_rune_master", "haste_book");
    }

    private void addToNpc(String npcId, String manualId) {
        npcInventory.computeIfAbsent(npcId, k -> new java.util.HashSet<>()).add(manualId);
    }

    public SecretManual get(String id) { return manuals.get(id); }

    /** 발견 — 정상 입수 경로. */
    public boolean discover(Player p, String manualId) {
        SecretManual m = manuals.get(manualId);
        if (m == null) { Msg.error(p, "비급 없음."); return false; }
        java.util.Set<String> set = owned.computeIfAbsent(p.getUniqueId(),
                k -> new java.util.HashSet<>());
        if (set.contains(manualId)) {
            Msg.warn(p, "이미 보유한 비급.");
            return false;
        }
        set.add(manualId);
        persistOwned(p.getUniqueId());
        Msg.send(p, "&5&l[비급 발견!] §f" + m.name + " §7(" + m.rarity.koreanName + ")");
        Msg.send(p, "&7/manual research " + manualId + " 로 연구 시작 (" + m.researchMinutes + "분)");
        if (m.rarity == SecretManual.Rarity.LEGENDARY || m.rarity == SecretManual.Rarity.MYTHIC) {
            Bukkit.broadcastMessage("§5§l[전설 비급] §f" + p.getName()
                    + " §7가 §6" + m.name + " §7를 발견했다!");
        }
        return true;
    }

    /** NPC로부터 도난 시도. */
    public boolean stealFromNpc(Player p, String npcId) {
        java.util.Set<String> npcSet = npcInventory.get(npcId);
        if (npcSet == null || npcSet.isEmpty()) {
            Msg.error(p, npcId + " 는 비급을 가지고 있지 않다.");
            return false;
        }
        double charisma = RebornCore.get().api().getStat(p.getUniqueId(),
                kr.reborn.core.data.StatType.AGILITY);
        double luck = RebornCore.get().api().getStat(p.getUniqueId(),
                kr.reborn.core.data.StatType.LUCK);
        double success = 0.05 + (charisma + luck) * 0.001;
        if (success > 0.5) success = 0.5;
        if (Rand.chance(success)) {
            String stolen = npcSet.iterator().next();
            npcSet.remove(stolen);
            discover(p, stolen);
            Msg.send(p, "&a&l[도난 성공] §7" + npcId + "의 비급");
            return true;
        }
        Msg.error(p, "&c도난 실패! NPC 호의 -50, 가까운 경비 출동.");
        try {
            var np = Bukkit.getPluginManager().getPlugin("RebornNPC");
            if (np != null) {
                np.getClass().getMethod("nudgeNearbyFavor",
                        Player.class, double.class, double.class)
                        .invoke(np, p, 30.0, -50.0);
            }
        } catch (Throwable ignored) {}
        return false;
    }

    /** 연구 시작. */
    public boolean startResearch(Player p, String manualId) {
        if (!ownsCheck(p, manualId)) return false;
        ensureResearchLoaded(p.getUniqueId());
        Map<String, Long> map = research.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        if (map.containsKey(manualId)) {
            Msg.warn(p, "이미 연구 중.");
            return false;
        }
        long now = System.currentTimeMillis();
        map.put(manualId, now);
        kr.reborn.core.RebornCore.get().kv().putLong(NS, p.getUniqueId(), "research." + manualId, now);
        SecretManual m = manuals.get(manualId);
        Msg.send(p, "&5연구 시작: " + m.name + " §7(" + m.researchMinutes + "분)");
        return true;
    }

    /** 연구 진척도 확인. */
    public double researchProgress(UUID p, String manualId) {
        ensureResearchLoaded(p);
        Long startedAt = research.getOrDefault(p, java.util.Collections.emptyMap()).get(manualId);
        if (startedAt == null) return 0;
        SecretManual m = manuals.get(manualId);
        if (m == null) return 0;
        long required = m.researchMinutes * 60_000L;
        long elapsed = System.currentTimeMillis() - startedAt;
        return Math.min(1.0, (double) elapsed / required);
    }

    /** 매 분 호출 — 완료된 연구 학습. */
    private void tickResearch() {
        for (var entry : research.entrySet()) {
            UUID uuid = entry.getKey();
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            var iter = entry.getValue().entrySet().iterator();
            while (iter.hasNext()) {
                var e = iter.next();
                SecretManual m = manuals.get(e.getKey());
                if (m == null) { iter.remove(); continue; }
                long elapsed = System.currentTimeMillis() - e.getValue();
                if (elapsed >= m.researchMinutes * 60_000L) {
                    // 완료
                    iter.remove();
                    kr.reborn.core.RebornCore.get().kv().remove(NS, uuid, "research." + e.getKey());
                    plugin.store().learn(uuid, m.skillId);
                    Msg.send(p, "&5&l[연구 완료] §f" + m.name + " §a→ 스킬 §e" + m.skillId + " §a습득!");
                }
            }
        }
    }

    private final java.util.Set<UUID> researchLoaded = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private void ensureResearchLoaded(UUID p) {
        if (researchLoaded.add(p)) {
            var all = kr.reborn.core.RebornCore.get().kv().loadAll(NS, p);
            Map<String, Long> map = new HashMap<>();
            for (var e : all.entrySet()) {
                if (e.getKey().startsWith("research.")) {
                    try { map.put(e.getKey().substring(9), Long.parseLong(e.getValue())); }
                    catch (Throwable ignored) {}
                }
            }
            if (!map.isEmpty()) research.put(p, map);
        }
    }

    /** 비급 양도 (판매). */
    public boolean transferTo(Player seller, Player buyer, String manualId) {
        var sSet = owned.get(seller.getUniqueId());
        if (sSet == null || !sSet.contains(manualId)) {
            Msg.error(seller, "보유 비급 없음.");
            return false;
        }
        sSet.remove(manualId);
        owned.computeIfAbsent(buyer.getUniqueId(), k -> new java.util.HashSet<>())
                .add(manualId);
        persistOwned(seller.getUniqueId());
        persistOwned(buyer.getUniqueId());
        Msg.send(seller, "&7비급 양도 완료: " + manualId);
        Msg.send(buyer, "&a비급 수령: " + manuals.get(manualId).name);
        return true;
    }

    public boolean ownsCheck(Player p, String manualId) {
        if (manuals.get(manualId) == null) { Msg.error(p, "비급 없음."); return false; }
        var set = owned.get(p.getUniqueId());
        if (set == null || !set.contains(manualId)) {
            Msg.error(p, "보유하지 않은 비급.");
            return false;
        }
        return true;
    }

    private static final String NS = "RebornSkill.manual";

    public java.util.Set<String> ownedOf(UUID p) {
        java.util.Set<String> cached = owned.get(p);
        if (cached != null) return cached;
        // DB 로드 — 쉼표 분리
        String stored = kr.reborn.core.RebornCore.get().kv().get(NS, p, "owned");
        if (stored == null || stored.isEmpty()) return java.util.Collections.emptySet();
        java.util.Set<String> loaded = new java.util.HashSet<>(java.util.Arrays.asList(stored.split(",")));
        owned.put(p, loaded);
        return loaded;
    }

    private void persistOwned(UUID p) {
        try {
            java.util.Set<String> set = owned.get(p);
            if (set == null) return;
            kr.reborn.core.RebornCore.get().kv().put(NS, p, "owned", String.join(",", set));
        } catch (Throwable ignored) {}
    }

    public List<SecretManual> available() {
        return new ArrayList<>(manuals.values());
    }

    public Map<String, Long> researchOf(UUID p) {
        ensureResearchLoaded(p);
        return research.getOrDefault(p, java.util.Collections.emptyMap());
    }
}
