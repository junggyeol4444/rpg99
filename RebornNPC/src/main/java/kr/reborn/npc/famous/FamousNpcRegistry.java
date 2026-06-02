package kr.reborn.npc.famous;

import kr.reborn.core.data.WorldKey;
import kr.reborn.npc.RebornNPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 유명 NPC 시드 (기획서 명명 인물).
 *
 * 60+ 시드:
 *   판타지: 아카니아 왕, 흑마법사 모르가스, 엘프 여왕, 드워프 왕
 *   마계: 마왕 바알/루시페르/사탄/마몬/레비아탄, 천마, 마장 발록
 *   천계: 천제, 4대천사 미카엘, 가브리엘, 라파엘, 우리엘
 *   무협: 천마, 무림맹주, 사파 거두, 검선 (검의 신선)
 *   선계: 태허선궁 궁주, 마선 두목, 도사 청풍
 *   요계: 요왕, 구미호 여왕, 오니 대장, 야쿠시지 텐구
 *   지구: 헌터협회장, S랭크 헌터 김민철, 게이트 길드장
 *   마도공학: 마도 황제, 노움 공학사
 *   아포: 군벌 칸, 약탈단 보스
 *   사이버: 메가코프 회장 알파/베타/감마
 *   드래곤: 고룡 아우렐리스, 청룡왕, 신룡 비아간
 *   해양: 해왕, 해적왕 블랙비어드, 인어 공주
 *   정령: 4 원소왕 (이프리나/운디네르/노움가르드/실피드론)
 */
public final class FamousNpcRegistry {

    private final RebornNPC plugin;
    private final Map<String, FamousNpc> npcs = new HashMap<>();

    public FamousNpcRegistry(RebornNPC plugin) {
        this.plugin = plugin;
        seed();
    }

    private void seed() {
        // ── 판타지 ──
        add("king_arcania", "&6&l아카니아 왕 라우엔", WorldKey.FANTASY,
                "KING", "kingdom_arcania", "왕", 8, "arteon_blade",
                "아카니아 왕국의 7대 왕. 정의롭고 현명하다.");
        add("morgath_dark_lord", "&8&l흑마법사 모르가스", WorldKey.FANTASY,
                "EMPEROR", "dark_alliance", "암흑의 군주", 9, "soul_drain",
                "암흑 동맹의 수장. 과거 왕족이었으나 타락.");
        add("elf_queen_lirielle", "&a&l엘프 여왕 리리엘", WorldKey.FANTASY,
                "QUEEN", "elf_kingdom", "여왕", 7, "illusion_legendary",
                "1000년을 다스린 엘프 여왕. 자연 마법의 정수.");
        add("dwarf_king_torin", "&6드워프 왕 토린", WorldKey.FANTASY,
                "KING", "dwarf_kingdom", "왕", 6, "haste_book",
                "산속 드워프 왕국의 군주. 룬 마스터.");
        add("mage_archmage", "&5&l대마법사 메를린", WorldKey.FANTASY,
                "ARCHMAGE", "mage_tower", "대마법사", 9, "meteor_grimoire",
                "마법사 탑의 7대 탑주. 모든 마법의 정수.");

        // ── 마계 — 7 마왕 ──
        add("marwang_lucifer", "&5&l[마왕] 루시페르", WorldKey.DEMON,
                "DEMON_LORD", "demon_lord_legion", "교만의 마왕", 10, "demon_pact",
                "교만의 마왕. 최초의 타락 천사.");
        add("marwang_satan", "&4&l[마왕] 사탄", WorldKey.DEMON,
                "DEMON_LORD", "demon_lord_legion", "분노의 마왕", 10, "hellfire_grimoire",
                "분노의 마왕. 끝없는 전쟁의 화신.");
        add("marwang_mammon", "&6&l[마왕] 마몬", WorldKey.DEMON,
                "DEMON_LORD", "demon_lord_legion", "탐욕의 마왕", 10, null,
                "탐욕의 마왕. 모든 부를 손에 쥔다.");
        add("marwang_leviathan", "&3&l[마왕] 레비아탄", WorldKey.DEMON,
                "DEMON_LORD", "demon_lord_legion", "질투의 마왕", 10, null,
                "질투의 마왕. 바다의 거대 존재.");
        add("marwang_baal", "&4&l[마왕] 바알", WorldKey.DEMON,
                "DEMON_LORD", "demon_lord_legion", "마왕의 우두머리", 10, null,
                "마왕 회의의 의장.");
        add("marwang_belial", "&8&l[마왕] 베리알", WorldKey.DEMON,
                "DEMON_LORD", "demon_lord_legion", "거짓의 마왕", 10, "nightmare_curse",
                "거짓의 마왕. 환영과 속임수의 화신.");
        add("marwang_asmodeus", "&d&l[마왕] 아스모데우스", WorldKey.DEMON,
                "DEMON_LORD", "demon_lord_legion", "색욕의 마왕", 10, null,
                "색욕의 마왕. 매혹의 화신.");

        // ── 천계 ──
        add("heaven_cheonje", "&6&l천제", WorldKey.HEAVEN,
                "EMPEROR", "celestial_court", "천제", 10, "celestial_judgment",
                "천계의 최고 신. 모든 신을 다스린다.");
        add("archangel_michael", "&e&l대천사 미카엘", WorldKey.HEAVEN,
                "ARCHANGEL", "celestial_court", "대천사", 9, "celestial_judgment",
                "정의의 대천사. 천계 군대의 총사령관.");
        add("archangel_gabriel", "&b&l대천사 가브리엘", WorldKey.HEAVEN,
                "ARCHANGEL", "celestial_court", "대천사", 9, "holy_radiance",
                "전령의 대천사. 신의 뜻을 전한다.");
        add("archangel_raphael", "&a&l대천사 라파엘", WorldKey.HEAVEN,
                "ARCHANGEL", "celestial_court", "대천사", 9, "purification",
                "치유의 대천사. 모든 상처를 낫게 한다.");
        add("archangel_uriel", "&c&l대천사 우리엘", WorldKey.HEAVEN,
                "ARCHANGEL", "celestial_court", "대천사", 9, null,
                "불의 대천사. 정화의 화염.");
        add("fallen_lucifer", "&8&l타락한 루시페르", WorldKey.HEAVEN,
                "FALLEN_ANGEL", "fallen_angels", "타락의 시조", 10, null,
                "한때 최고 대천사였으나 타락. 현재 마계 거주.");

        // ── 무협 ──
        add("cheonma", "&5&l천마", WorldKey.MARTIAL,
                "CULT_MASTER", "demon_cult", "천마", 10, "hyeolma_gong",
                "마교의 절대 교주. 천마신공의 창시자.");
        add("alliance_master_namgung", "&3&l무림맹주 남궁세가주", WorldKey.MARTIAL,
                "ALLIANCE_MASTER", "wulin_alliance", "무림맹주", 9, "hangryong_18",
                "남궁세가의 가주이자 정파 무림맹의 수장.");
        add("sword_immortal_dokgo", "&6&l검선 독고검존", WorldKey.MARTIAL,
                "MASTER", "wanderer", "검선", 10, "dokgo_guzeom",
                "검의 신선. 독고구검의 창시자. 은둔 중.");
        add("sapa_lord_yul", "&4&l사파 거두 율삼태", WorldKey.MARTIAL,
                "MASTER", "sapa_union", "사파 거두", 8, "sura_magong",
                "사파 연합의 수장. 무자비하나 신의 있다.");
        add("solim_abbot", "&5&l소림 방장 무진대사", WorldKey.MARTIAL,
                "ABBOT", "shaolin", "방장", 9, "guyang_jin",
                "소림 7대 방장. 구양진경의 전수자.");
        add("wudang_taoist", "&6무당 장로 청풍", WorldKey.MARTIAL,
                "TAOIST", "wudang", "장로", 8, "taegeuk_kwon",
                "무당 9대 장로. 태극권의 전수자.");

        // ── 선계 ──
        add("taeheo_master", "&5&l태허선궁 궁주", WorldKey.IMMORTAL,
                "PALACE_MASTER", "taoist_alliance", "선궁주", 10, "time_manipulation",
                "선계 정도의 수장. 만선의 우두머리.");
        add("demon_sage_overlord", "&5&l마선 두목 흑령선군", WorldKey.IMMORTAL,
                "MASTER", "demon_immortals", "마선왕", 9, null,
                "마선들의 우두머리. 정선과 영원한 적대.");

        // ── 요계 ──
        add("yokai_emperor", "&5&l[요제] 야쿠모", WorldKey.YOKAI,
                "EMPEROR", "yokai_imperial", "요제", 10, "yokai_transformation",
                "요계의 황제. 9꼬리 구미호의 후예.");
        add("kumiho_queen", "&d&l구미호 여왕 화린", WorldKey.YOKAI,
                "QUEEN", "fox_clan", "구미호 여왕", 9, "yokai_transformation",
                "구미호 일족의 여왕. 변신의 극의.");
        add("oni_lord", "&c&l오니 대장 쿠라마", WorldKey.YOKAI,
                "MASTER", "oni_horde", "오니 대장", 9, null,
                "오니 군단의 수장. 무력의 화신.");
        add("tengu_master", "&8&l텐구 일족장 야쿠시지", WorldKey.YOKAI,
                "MASTER", "tengu_clan", "텐구 종주", 8, null,
                "산속 텐구 일족의 종주.");

        // ── 지구 ──
        add("hunter_association_head", "&b&l헌터협회장 강철수", WorldKey.EARTH,
                "ASSOC_HEAD", "korean_assoc", "협회장", 9, null,
                "한국 헌터 협회의 7대 회장. S랭크 베테랑.");
        add("s_rank_hunter_kim", "&e&lS랭크 헌터 김민철", WorldKey.EARTH,
                "HUNTER", "korean_assoc", "S랭크", 9, null,
                "전국 1위 헌터. 검과 마법을 동시에 다룬다.");
        add("gate_guild_master", "&6게이트 길드장", WorldKey.EARTH,
                "GUILD_MASTER", "korean_assoc", "길드장", 8, null,
                "최대 게이트 길드의 수장.");

        // ── 마도공학 ──
        add("magitech_emperor", "&5&l마도 황제 그란드 그놈", WorldKey.MAGITECH,
                "EMPEROR", "machine_lord", "마도 황제", 10, null,
                "마도공학 세계의 황제. 천재 노움 공학사.");
        add("gnome_engineer_chief", "&6노움 공학사 마스터", WorldKey.MAGITECH,
                "MASTER", "gnome_engineers", "수석 공학사", 8, null,
                "노움 공학단의 수석 공학사.");

        // ── 아포 ──
        add("warlord_khan", "&4&l군벌 칸", WorldKey.APOCALYPSE,
                "WARLORD", "warlord_state", "군벌", 9, null,
                "폐허 최강의 군벌. 잔인하나 부하 신의 깊다.");
        add("raider_boss", "&8약탈단 보스", WorldKey.APOCALYPSE,
                "BOSS", "raider_clan", "약탈단장", 7, null,
                "약탈단의 수장. 무자비한 살수.");
        add("survivor_elder", "&7생존자 장로", WorldKey.APOCALYPSE,
                "ELDER", "survivor_camp", "원로", 6, null,
                "정착지의 정신적 지주.");

        // ── 사이버펑크 ──
        add("megacorp_alpha_ceo", "&b&l메가코프 알파 회장", WorldKey.CYBERPUNK,
                "CEO", "megacorp_alpha", "회장", 9, "cyber_master_hack",
                "최대 메가코프의 회장. 정치·경제 모두 장악.");
        add("megacorp_beta_ceo", "&5메가코프 베타 회장", WorldKey.CYBERPUNK,
                "CEO", "megacorp_beta", "회장", 8, null,
                "AI 무기 전문 기업의 회장.");
        add("megacorp_gamma_ceo", "&6메가코프 감마 회장", WorldKey.CYBERPUNK,
                "CEO", "megacorp_gamma", "회장", 8, null,
                "사이버 임플란트 전문 기업의 회장.");
        add("netbreaker_legend", "&dNetBreaker 전설", WorldKey.CYBERPUNK,
                "HACKER", "netbreakers", "전설", 9, "cyber_master_hack",
                "익명의 전설적 해커. 모든 기업이 추격 중.");

        // ── 드래곤 ──
        add("dragon_king_aurelius", "&6&l[용왕] 아우렐리스", WorldKey.DRAGON,
                "DRAGON_LORD", "elder_drakes", "용왕", 10, "dragon_breath_advanced",
                "드래곤 세계의 군주. 5000년을 다스렸다.");
        add("ancient_dragon_red", "&c&l적룡 이그니스", WorldKey.DRAGON,
                "ELDER_DRAGON", "elder_drakes", "고룡", 9, null,
                "화염의 고룡. 산속 둥지에 거주.");
        add("ancient_dragon_blue", "&3&l청룡 아쿠라", WorldKey.DRAGON,
                "ELDER_DRAGON", "elder_drakes", "고룡", 9, null,
                "물의 고룡. 깊은 호수의 군주.");
        add("ancient_dragon_gold", "&6&l금룡 솔라리스", WorldKey.DRAGON,
                "ELDER_DRAGON", "elder_drakes", "고룡", 9, null,
                "선의 고룡. 보호자 성향.");
        add("ancient_dragon_black", "&8&l흑룡 노크투르나", WorldKey.DRAGON,
                "ELDER_DRAGON", "elder_drakes", "고룡", 9, null,
                "악의 고룡. 어둠 속에 거주.");

        // ── 해양 ──
        add("sea_king", "&3&l[해왕] 트리톤", WorldKey.OCEAN,
                "EMPEROR", "atlantis_court", "해왕", 10, "sea_dragon_array",
                "해양 제국의 황제. 바다의 절대자.");
        add("pirate_king_blackbeard", "&6&l[해적왕] 블랙비어드", WorldKey.OCEAN,
                "PIRATE_KING", "pirate_brotherhood", "해적왕", 9, null,
                "7대해를 누비는 해적왕. 자유의 화신.");
        add("merfolk_princess", "&b&l인어 공주", WorldKey.OCEAN,
                "PRINCESS", "atlantis_court", "공주", 8, null,
                "해왕의 딸. 노래로 폭풍을 잠재운다.");
        add("kraken_lord", "&5&l크라켄의 군주", WorldKey.OCEAN,
                "BOSS", "deep_dwellers", "심해의 군주", 9, null,
                "심해에 숨은 거대 생물. 가끔 해적선을 공격.");

        // ── 정령 4 원소왕 ──
        add("elemental_king_fire", "&c&l[원소왕] 이프리나", WorldKey.SPIRIT,
                "ELEMENTAL_KING", "spirit_kings", "화염 원소왕", 10, null,
                "화염 정령의 절대 군주.");
        add("elemental_king_water", "&3&l[원소왕] 운디네르", WorldKey.SPIRIT,
                "ELEMENTAL_KING", "spirit_kings", "수 원소왕", 10, null,
                "수 정령의 절대 군주.");
        add("elemental_king_earth", "&6&l[원소왕] 노움가르드", WorldKey.SPIRIT,
                "ELEMENTAL_KING", "spirit_kings", "지 원소왕", 10, null,
                "지 정령의 절대 군주.");
        add("elemental_king_wind", "&a&l[원소왕] 실피드론", WorldKey.SPIRIT,
                "ELEMENTAL_KING", "spirit_kings", "풍 원소왕", 10, "spirit_king_summon",
                "풍 정령의 절대 군주.");
        add("primordial_light", "&f&l태초의 빛 루미엘", WorldKey.SPIRIT,
                "PRIMORDIAL", "primordial_spirits", "절대 정령", 10, null,
                "빛의 태초 정령. 모든 정령의 시조.");
        add("primordial_dark", "&0&l태초의 어둠 옴브라", WorldKey.SPIRIT,
                "PRIMORDIAL", "primordial_spirits", "절대 정령", 10, null,
                "어둠의 태초 정령. 모든 정령의 시조.");
    }

    private void add(String id, String name, WorldKey w, String job, String faction,
                     String title, int rank, String reward, String desc) {
        npcs.put(id, new FamousNpc(id, name, w, job, faction, title, rank, reward, desc));
    }

    public FamousNpc get(String id) { return npcs.get(id); }
    public java.util.Collection<FamousNpc> all() { return npcs.values(); }

    public List<FamousNpc> ofWorld(WorldKey w) {
        List<FamousNpc> list = new ArrayList<>();
        for (FamousNpc n : npcs.values()) if (n.world == w) list.add(n);
        return list;
    }

    public List<FamousNpc> topRank(int n) {
        return npcs.values().stream()
                .sorted((a, b) -> Integer.compare(b.powerRank, a.powerRank))
                .limit(n)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 시드된 50+ 유명 NPC를 실제 NpcRegistry에 spawn 등록.
     * RebornNPC onEnable 후 자동 호출. 이미 spawn된 famous NPC는 스킵.
     * 위치: 해당 세계의 첫 로드된 청크 (월드 spawn 근처).
     */
    public int spawnAllToWorld() {
        int spawned = 0;
        for (FamousNpc fn : npcs.values()) {
            if (plugin.registry().get(fn.id) != null) continue;  // 이미 등록됨
            // 해당 세계명과 일치하는 Bukkit 월드 찾기 (소문자 매칭)
            org.bukkit.World world = null;
            for (org.bukkit.World w : org.bukkit.Bukkit.getWorlds()) {
                if (w.getName().equalsIgnoreCase(fn.world.name())
                        || w.getName().toLowerCase().contains(fn.world.name().toLowerCase())) {
                    world = w; break;
                }
            }
            if (world == null) continue;  // 해당 월드가 로드 안 됨
            org.bukkit.Location loc = world.getSpawnLocation().clone()
                    .add(Math.random() * 30 - 15, 0, Math.random() * 30 - 15);
            try {
                var npc = plugin.registry().spawn(fn.id, fn.displayName, fn.world, loc, fn.faction, fn.job);
                if (npc != null) {
                    // 권력 등급에 따라 스탯 강화
                    npc.stats.put("STRENGTH", (double) fn.powerRank * 1000);
                    npc.stats.put("ENDURANCE", (double) fn.powerRank * 1000);
                    npc.stats.put("CHARISMA", (double) fn.powerRank * 100);
                    spawned++;
                }
            } catch (Throwable ignored) {}
        }
        if (spawned > 0) {
            plugin.getLogger().info("Famous NPC 자동 spawn: " + spawned + "/" + npcs.size());
        }
        return spawned;
    }
}
