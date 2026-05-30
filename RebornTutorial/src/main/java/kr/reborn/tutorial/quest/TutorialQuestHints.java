package kr.reborn.tutorial.quest;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import kr.reborn.tutorial.RebornTutorial;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 튜토리얼 퀘스트 힌트 — 단계 진입 시 세계별 첫 추천 행동 안내.
 *
 * 단계 1: 기본 학습 (보호 구역 둘러보기, /rtut train 사용)
 * 단계 2: 도전 (튜토리얼 NPC 1체 처치, 첫 스킬 시전)
 * 단계 3: 출국 준비 (헬스장 운동, 본 세계 정보 미리보기)
 *
 * 본 세계별 첫 권장 행동도 표시 (기획서 4장 참조).
 */
public final class TutorialQuestHints {

    private final RebornTutorial plugin;
    private final Map<WorldKey, List<String>> worldHints = new HashMap<>();

    public TutorialQuestHints(RebornTutorial plugin) {
        this.plugin = plugin;
        seedHints();
    }

    private void seedHints() {
        worldHints.put(WorldKey.FANTASY, List.of(
                "&7판타지 — 마법 학파 6종 중 하나를 선택하라",
                "&7마법사 탑에서 룬 수집을 시작하라",
                "&7종족 보너스를 활용하라 (ELF=MANA, DWARF=END)"));
        worldHints.put(WorldKey.DEMON, List.of(
                "&7마계 — 마기 흡수가 핵심. 영혼 처치 시 마기 +",
                "&7마기 1000+ = 마인 단계 진입",
                "&7마왕 후계 자격은 마기 20000+ 필요"));
        worldHints.put(WorldKey.HEAVEN, List.of(
                "&7천계 — 살생 금기. NPC 신전에 기도하라",
                "&7신성 1000+ = 대천사 후보",
                "&7타락한 동료를 만나면 구원·처단·동조 3선택"));
        worldHints.put(WorldKey.SPIRIT, List.of(
                "&7정령계 — 6 원소 친화도 시스템",
                "&7정신력 0 = 즉시 소멸. 항상 명상 유지",
                "&7정령왕 호의로 친화도 가속"));
        worldHints.put(WorldKey.MARTIAL, List.of(
                "&7무협 — 사냥 무의미. 운기조식·비급·단약이 전부",
                "&7정파/사파/마교 분기 선택",
                "&7깨달음 9단계 = 무공의 극의"));
        worldHints.put(WorldKey.IMMORTAL, List.of(
                "&7선계 — 천기 1000마다 천겁",
                "&7실패시 선계 천벌 (수련 효율 -80%)",
                "&7도반과 합동 양생 시 +30%"));
        worldHints.put(WorldKey.YOKAI, List.of(
                "&7요계 — 밤 3배, 보름달 10배",
                "&7꼬리 1~9 단계 (요기 1000마다)",
                "&79꼬리 = 구미호 강림 (절대자)"));
        worldHints.put(WorldKey.EARTH, List.of(
                "&7지구 — 헌터 협회 명성 누적",
                "&7각인 카드 10종마다 모든 스탯 +5",
                "&7S랭크 게이트 = 큰 보상"));
        worldHints.put(WorldKey.MAGITECH, List.of(
                "&7마도공학 — 5 코어 등급",
                "&7같은 등급 3개 = 다음 등급 합성",
                "&7코어 가중치 100+ = 과부하 위험"));
        worldHints.put(WorldKey.APOCALYPSE, List.of(
                "&7폐허 — 방사능 누적 100 = 저주",
                "&7식수·식량 2주 비축 = 정신·체력 보너스",
                "&7명상 30% 확률로 변종 습격"));
        worldHints.put(WorldKey.CYBERPUNK, List.of(
                "&7사이버펑크 — 임플란트 8 슬롯",
                "&74+ 의존증, 7+ 사이코시스",
                "&7해킹 quality로 데이터 칩 발견"));
        worldHints.put(WorldKey.DRAGON, List.of(
                "&7드래곤 — 잠·보물·나이가 핵심",
                "&7나이 5단계 (어린용→청룡→장룡→고룡→신룡)",
                "&7다른 용 처치 시 용력 30% 흡수"));
        worldHints.put(WorldKey.OCEAN, List.of(
                "&7해양 — 바다에서만 성장",
                "&7해양력 1000 = 항해사 + WATER_BREATHING",
                "&7적선 나포로 명성"));
    }

    /** 본 세계 정보 표시. */
    public void showWorldGuide(Player p) {
        try {
            var data = RebornCore.get().api().getPlayerData(p.getUniqueId());
            if (data == null) return;
            WorldKey w = data.worldKey();
            List<String> hints = worldHints.get(w);
            if (hints == null) return;
            Msg.send(p, "&6&l=== " + w + " 세계 가이드 ===");
            for (String h : hints) p.sendMessage(h);
        } catch (Throwable ignored) {}
    }

    /** 단계 1 진입 시 표시. */
    public void stage1Hints(Player p) {
        Msg.send(p, "&e&l[튜토리얼 단계 1 - 기본 학습]");
        p.sendMessage("§7• §f/rtut train §7- 30초마다 모든 기본 스탯 +2");
        p.sendMessage("§7• §f/skill list §7- 자동 지급된 스킬 확인");
        p.sendMessage("§7• §f/hc abilities §7- 히든 클래스 능력 확인 (해금 시)");
    }

    /** 단계 2 진입 시 표시. */
    public void stage2Hints(Player p) {
        Msg.send(p, "&6&l[튜토리얼 단계 2 - 도전]");
        p.sendMessage("§7• 보호 구역 밖으로 나가 챔피언 NPC와 전투");
        p.sendMessage("§7• 첫 스킬 시전 — /skill cast <id>");
        p.sendMessage("§7• 사망 시 신들의 심부름꾼/명계 선택");
    }

    /** 단계 3 진입 시 표시. */
    public void stage3Hints(Player p) {
        Msg.send(p, "&6&l[튜토리얼 단계 3 - 출국 준비]");
        p.sendMessage("§7• §f/gym §7- 신들의 헬스장에서 운동 마무리");
        showWorldGuide(p);
        p.sendMessage("§a/rtut exit §7- 본 세계로 진입");
    }
}
