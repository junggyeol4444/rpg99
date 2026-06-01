package kr.reborn.npc.interact;

import kr.reborn.core.util.Rand;
import kr.reborn.npc.entity.RebornNpc;
import kr.reborn.npc.soul.Personality;

import java.util.List;

/**
 * NPC 인사·응답 변형 사전.
 *
 * 기존: 6 고정 인사말 (호감/적대/자존심/공감/사교성)
 * 변경: 각 상황마다 3~5 변형, 무작위 선택 + 직업·세계 가산
 *
 * 결과: 같은 NPC를 여러 번 만나도 거의 매번 다른 인사.
 */
public final class ResponseBank {

    /** 호감 매우 높음 (60+) */
    private static final List<String> WARM = List.of(
            "오, 자네 왔는가! 늘 반갑네.",
            "어서 오시게. 자네를 기다렸다네.",
            "또 만났구먼! 차 한 잔 하고 가시게.",
            "자네라면 반갑지. 무슨 일인가?",
            "기쁘게 맞이하리다. 편히 머무시오."
    );

    /** 호감 매우 낮음 (-40 이하) */
    private static final List<String> COLD = List.of(
            "...왜 또 왔나.",
            "꺼져라.",
            "또 자넨가. 보고 싶지 않다.",
            "그만 가시오. 더는 말 섞지 않겠소.",
            "...."
    );

    /** 자존심 높음 (50+) */
    private static final List<String> PROUD = List.of(
            "감히 나에게 말을 거는가?",
            "내 앞에 무릎을 꿇어라.",
            "너 따위가 무슨 일로?",
            "내 시간은 귀하다. 짧게 말하라.",
            "예의를 갖춰라."
    );

    /** 공감 높음 (50+) */
    private static final List<String> EMPATHIC = List.of(
            "오, 반갑네. 무엇이 필요한가?",
            "어떤 곤란한 일이 있는가? 들어주리다.",
            "마음이 무거워 보이는군. 앉으시게.",
            "도울 일이 있다면 말해 주시게.",
            "반가운 손님이로다. 차 한 잔 어떤가?"
    );

    /** 사교성 낮음 (-30 이하) */
    private static final List<String> ANTISOCIAL = List.of(
            "...",
            "(고개를 끄덕인다)",
            "(말없이 쳐다본다)",
            "(돌아선다)",
            "용건만 말하라."
    );

    /** 보통 — 무난한 인사 */
    private static final List<String> NEUTRAL = List.of(
            "무슨 일이오?",
            "용건이 있으신가?",
            "처음 뵙는구려.",
            "어디서 오셨소?",
            "...무슨 일인가?",
            "어찌 오셨는지?"
    );

    /** 직업별 추가 인사 */
    public static String jobGreeting(String job) {
        switch (job) {
            case "MERCHANT":
                return pick("어서 오시오! 물건 한 번 보겠소?",
                        "오, 손님이군. 좋은 물건이 있다오.",
                        "거래하러 오셨는가?");
            case "BLACKSMITH":
                return pick("쟁쟁! 망치질 중이오. 잠깐만.",
                        "검을 손보러 왔는가?",
                        "철의 향이 좋군. 무얼 도와드릴까?");
            case "GUARD":
                return pick("경계 중이오. 무슨 일인가?",
                        "수상한 자는 아니겠지?",
                        "신원을 확인할 필요가 있나?");
            case "PRIEST":
                return pick("신의 가호가 있기를.",
                        "신앙을 가지고 오셨는가?",
                        "고민이 있다면 기도하시게.");
            case "HERMIT":
                return pick("...노부에게 무슨 일인고?",
                        "여기까지 찾아왔는가. 어찌 오셨소?",
                        "(눈을 감고 있다)");
            case "FARMER":
                return pick("밭을 갈고 있소. 무슨 일이오?",
                        "땀에 젖은 채로 인사하기 죄송하오.",
                        "농사일이 바빠서 말이오.");
            case "KING": case "EMPEROR":
                return pick("짐 앞에 무릎을 꿇어라.",
                        "왕가에 용건이 있는가?",
                        "감히 짐을 알현하려는가?");
            case "CULT_MASTER":
                return pick("천마님께 인사하라.",
                        "마교의 일원이 되고 싶은가?",
                        "그대의 마음에 마기가 흐르는군.");
            case "ALLIANCE_MASTER":
                return pick("무림에 들어선 자여, 정의의 길을 걷겠는가?",
                        "정파의 일원이 되고자 하는가?",
                        "강호의 명예를 아는 자인가?");
            case "DEMON_LORD":
                return pick("작은 자야, 어찌 내 옥좌에 다다랐는가?",
                        "감히 마왕 앞에 서다니.",
                        "재미있군. 너의 용기를 시험하리라.");
            default:
                return null;
        }
    }

    /** 호감·성격에 따른 인사 선택. */
    public static String pickGreeting(RebornNpc npc, double sentiment) {
        if (npc.soul == null) return pick(NEUTRAL);
        int empathy = npc.soul.personality.get(Personality.Trait.EMPATHY);
        int pride = npc.soul.personality.get(Personality.Trait.PRIDE);
        int soc = npc.soul.personality.get(Personality.Trait.SOCIABILITY);

        if (sentiment > 60) return pick(WARM);
        if (sentiment < -40) return pick(COLD);
        if (pride > 50) return pick(PROUD);
        if (empathy > 50) return pick(EMPATHIC);
        if (soc < -30) return pick(ANTISOCIAL);
        return pick(NEUTRAL);
    }

    public static String pick(List<String> list) {
        return list.get(Rand.range(0, list.size() - 1));
    }

    private static String pick(String... opts) {
        return opts[Rand.range(0, opts.length - 1)];
    }
}
