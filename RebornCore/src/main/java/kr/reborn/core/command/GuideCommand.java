package kr.reborn.core.command;

import kr.reborn.core.RebornCore;
import kr.reborn.core.data.PlayerData;
import kr.reborn.core.data.WorldKey;
import kr.reborn.core.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /guide — 현재 세계에 맞는 시스템·명령어 안내.
 *
 * 신규 사용자 온보딩 핵심: 자기 세계에 어떤 콘텐츠/명령이 있는지 한눈에.
 * 세계별로 다른 안내 텍스트를 출력한다 (공통 + 세계 특화).
 */
public final class GuideCommand implements CommandExecutor {

    private final RebornCore plugin;
    public GuideCommand(RebornCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) { Msg.error(s, "플레이어 전용"); return true; }
        PlayerData d = RebornCore.get().api().getPlayerData(p.getUniqueId());
        WorldKey w = d == null ? WorldKey.LOBBY : d.worldKey();

        Msg.send(p, "&6===== 환생의 월드 안내 (" + w + ") =====");
        // 공통
        Msg.send(p, "&e[공통] &7/stats 스탯 · /tierup 경지 돌파 · /fortune 기연");
        Msg.send(p, "&e      &7/skill 스킬 · /hidden 히든월드 · /pastlife 전생");
        Msg.send(p, "&e      &7/clan 가문 · /marry 결혼 · /child 자녀 · /title 칭호");

        // 세계 특화 안내
        for (String line : worldGuide(w)) p.sendMessage(line);

        Msg.send(p, "&7더 궁금하면 각 명령을 인자 없이 입력해 보세요.");
        return true;
    }

    private String[] worldGuide(WorldKey w) {
        switch (w) {
            case MARTIAL:
                return new String[]{
                        "&b[무협] &7/meditate 운기조식 · /school 학파 가입 (정파/사파/마교/황궁/은둔)",
                        "&b      &7/manual 비급 연구 · /arraymeditate 진법 합동수련",
                        "&7무공 비급은 학파 제한이 있다. 정파↔마교 상극."};
            case CYBERPUNK:
                return new String[]{
                        "&b[사이버펑크] &7/corp 7대 메가코프 평판 (/corp gui)",
                        "&b           &7/district 7대 구역 점령전",
                        "&7후원 코프 의뢰를 수행하면 구역 영향력이 쌓여 점령 가능."};
            case OCEAN:
                return new String[]{
                        "&b[해양] &7/empire 7대 해양 제국 평판 · /port 7대 항구 점령전",
                        "&7다이빙·진주 채집·해전·나포로 해양력을 키운다."};
            case SPIRIT:
                return new String[]{
                        "&b[정령] &7/element 주 속성 선택 (4대+12소원소) · /petition 원소왕 청원",
                        "&7정신력이 부족하면 카오스 접촉 시 폭주한다."};
            case YOKAI:
                return new String[]{
                        "&b[요계] &7/transform 변신 · /moonritual 보름달 의식",
                        "&7요기를 쌓아 구미호·오니 등의 힘을 다룬다."};
            case HEAVEN:
                return new String[]{
                        "&b[천계] &7/pray 신전 기도",
                        "&7신성을 쌓으면 신계(GOD) 승천의 길이 열린다. /god 참조."};
            case DEMON:
                return new String[]{
                        "&b[마계] &7/stabilize 마기 안정화",
                        "&7마기가 과하면 정신을 침식한다. 마왕의 길을 걸어라."};
            case IMMORTAL:
                return new String[]{
                        "&b[선계] &7/meditate 양생 · /arraymeditate 도반 합동수련",
                        "&736동천·72복지를 탐험해 선기를 쌓는다."};
            case DRAGON:
                return new String[]{
                        "&b[드래곤] &7브레스로 싸우고 비행한다. 용력을 쌓아 용왕에 도전.",
                        "&7용 로드 가문의 호의를 얻으면 브레스 비급을 받는다."};
            case GOD:
                return new String[]{
                        "&b[신계] &7/god ascend 신 등극 · /god miracle 기적 · /god religion 교단",
                        "&7신도의 신앙이 곧 너의 신성이 된다. /god religion ritual 집전."};
            case EARTH:
                return new String[]{
                        "&b[지구] &7게이트·던전·미궁을 공략하라. 헌터 협회 의뢰.",
                        "&7각성석·게이트 유물로 잠재력을 깨운다."};
            case MAGITECH:
                return new String[]{
                        "&b[마도공학] &7마도 코어·터렛·레이저를 제작. /specialty 마도공학.",
                        "&7마나와 기계의 융합으로 싸운다."};
            case APOCALYPSE:
                return new String[]{
                        "&b[종말] &7생존이 우선. 방사능·변종에 맞서라.",
                        "&7변이 혈청으로 신체를 강화한다."};
            case FANTASY:
                return new String[]{
                        "&b[판타지] &7마법서로 스킬을 익히고 원소를 다룬다.",
                        "&7서클을 올려 9서클 대마법사에 이른다."};
            default:
                return new String[]{
                        "&7세계를 선택하면 그 세계 전용 시스템 안내가 표시됩니다."};
        }
    }
}
