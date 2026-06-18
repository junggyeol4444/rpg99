package kr.reborn.skill.command;

import kr.reborn.core.util.Msg;
import kr.reborn.skill.RebornSkill;
import kr.reborn.skill.school.MartialSchool;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class SchoolCommand implements CommandExecutor {
    private final RebornSkill plugin;
    public SchoolCommand(RebornSkill plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c,
                             @NotNull String l, @NotNull String[] a) {
        if (!(s instanceof Player p)) return true;
        if (a.length == 0) {
            MartialSchool ms = plugin.schools().of(p.getUniqueId());
            Msg.send(p, "&6내 학파: " + (ms != null ? ms.colorCode + ms.koreanName : "&7무소속"));
            Msg.send(p, "&7/school join <ORTHODOX|UNORTHODOX|DEMON_CULT|IMPERIAL|HERMIT>");
            Msg.send(p, "&7/school list                - 5 학파 정보");
            Msg.send(p, "&7/school gui                 - 학파 선택 GUI");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "gui" -> openGui(p);
            case "join" -> {
                if (a.length < 2) { Msg.warn(p, "/school join <SCHOOL>"); return true; }
                try {
                    MartialSchool ms = MartialSchool.valueOf(a[1].toUpperCase());
                    plugin.schools().setSchool(p, ms);
                } catch (Exception e) { Msg.error(p, "잘못된 학파."); }
            }
            case "list" -> {
                Msg.send(p, "&6=== 5 무공 학파 ===");
                for (MartialSchool ms : MartialSchool.values()) {
                    p.sendMessage("§7• " + ms.colorCode + ms.koreanName
                            + " §7- §e" + ms.bonus);
                }
            }
            default -> Msg.warn(p, "/school gui|join|list");
        }
        return true;
    }

    /**
     * 5 무공 학파 GUI — 학파별 테마 아이템·보너스 lore·현재 학파 ★ 표시.
     * 클릭 → 학파 가입 (/school join).
     */
    private void openGui(Player p) {
        var b = plugin.gui().builder("&65 무공 학파", 3);
        MartialSchool current = plugin.schools().of(p.getUniqueId());
        // 학파별 테마 — 정파(다이아소드)·사파(골든소드)·마교(네더라이트소드)·황궁(왕관)·은둔(나뭇잎).
        org.bukkit.Material[] mats = {
                org.bukkit.Material.DIAMOND_SWORD,
                org.bukkit.Material.GOLDEN_SWORD,
                org.bukkit.Material.NETHERITE_SWORD,
                org.bukkit.Material.GOLDEN_HELMET,
                org.bukkit.Material.OAK_SAPLING
        };
        // 학파 중앙 정렬 — slot 11, 12, 13, 14, 15 (3행 9칸 중간 행 중앙 5칸).
        int[] slots = {11, 12, 13, 14, 15};
        MartialSchool[] schools = MartialSchool.values();
        for (int i = 0; i < schools.length && i < slots.length; i++) {
            final MartialSchool ms = schools[i];
            boolean isCurrent = ms == current;
            StringBuilder bonusLine = new StringBuilder();
            int n = 0;
            for (var e : ms.bonus.entrySet()) {
                if (n++ > 0) bonusLine.append(", ");
                bonusLine.append(e.getKey().name()).append(" +").append(e.getValue().intValue());
            }
            var item = kr.reborn.core.util.Items.of(
                    mats[i],
                    (isCurrent ? "&e★ " : "") + ms.colorCode + ms.koreanName,
                    "&7" + ms.name(),
                    "&e보너스: &f" + bonusLine,
                    isCurrent ? "&e현재 소속" : "",
                    "",
                    "&a클릭 — 가입 (24시간 쿨다운)");
            b.set(slots[i], item, e -> {
                p.closeInventory();
                plugin.schools().setSchool(p, ms);
            });
        }
        b.open(p);
    }
}
