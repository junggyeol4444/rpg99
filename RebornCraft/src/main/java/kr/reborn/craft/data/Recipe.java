package kr.reborn.craft.data;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public final class Recipe {

    public static final class Mat {
        public final Material material;
        public final int amount;
        public Mat(Material m, int a) { this.material = m; this.amount = a; }
    }

    public final String id;
    public final String profession;
    public final int minProficiency;
    public final List<Mat> materials = new ArrayList<>();
    public final String resultItemId;
    public final int castSeconds;
    public final double successRate;
    public final double higherGradeChance;
    public final int expGain;

    public Recipe(String id, String profession, int minProficiency,
                  String resultItemId, int castSeconds, double successRate,
                  double higherGradeChance, int expGain) {
        this.id = id; this.profession = profession; this.minProficiency = minProficiency;
        this.resultItemId = resultItemId; this.castSeconds = castSeconds;
        this.successRate = successRate; this.higherGradeChance = higherGradeChance;
        this.expGain = expGain;
    }
}
