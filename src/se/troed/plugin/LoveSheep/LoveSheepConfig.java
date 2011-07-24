package se.troed.plugin.LoveSheep;

import org.bukkit.DyeColor;
import org.bukkit.util.config.Configuration;

import java.util.*;


public class LoveSheepConfig {

    private Integer distance;
    private Integer maxLove;
    private Double bigamyChance;
    private DyeColor sheepColor;

    public Integer getDistance() {
        return distance;
    }

    public Integer getMaxLove() {
        return maxLove;
    }

    public Double getBigamyChance() {
        return bigamyChance;
    }

    public DyeColor getSheepColor() {
        return sheepColor;
    }

    public LoveSheepConfig(Configuration config) {

        distance = (Integer)config.getProperty("distance");
        maxLove = (Integer)config.getProperty("maxLove");
        bigamyChance = (Double)config.getProperty("bigamyChance");
        sheepColor = DyeColor.getByData((Byte)config.getProperty("sheepColor"));
    }
}
