package com.faceunity.beautycontrolview.entity;



import com.faceunity.fup2a.R;

import java.util.ArrayList;

/**
 * Created by tujh on 2018/1/30.
 */

public enum EffectEnum {
    Effect_zhangxiaofan("zhangxiaofan", R.drawable.zhangxiaofan, "normal/zhangxiaofan.bundle", 4, Effect.EFFECT_TYPE_NORMAL, ""),
    Effect_qiuxiaohao("qiuxiaohao", R.drawable.qiuxiaohao, "normal/qiuxiaohao.bundle", 4, Effect.EFFECT_TYPE_NORMAL, ""),
    Effect_mieba("mieba", R.drawable.mieba, "normal/mieba.bundle", 4, Effect.EFFECT_TYPE_NORMAL, ""),
    Effect_xiong("xiong", R.drawable.xiong, "normal/xiong.bundle", 4, Effect.EFFECT_TYPE_NORMAL, ""),
    Effect_shengdanlaoren("shengdanlaoren", R.drawable.shengdanlaoren, "normal/shengdanlaoren.bundle", 1, Effect.EFFECT_TYPE_NORMAL, ""),
    Effect_chaiquan("chaiquan", R.drawable.chaiquan, "normal/chaiquan.bundle", 4, Effect.EFFECT_TYPE_NORMAL, ""),
    Effect_mao("mao", R.drawable.mao, "normal/mao.bundle", 4, Effect.EFFECT_TYPE_NORMAL, ""),
    Effect_gou("gou", R.drawable.gou, "normal/gou.bundle", 4, Effect.EFFECT_TYPE_NORMAL, ""),
    Effect_tiger("tiger", R.drawable.tiger, "normal/tiger.bundle", 4, Effect.EFFECT_TYPE_NORMAL, "");


    private String bundleName;
    private int resId;
    private String path;
    private int maxFace;
    private int effectType;
    private String description;

    EffectEnum(String name, int resId, String path, int maxFace, int effectType, String description) {
        this.bundleName = name;
        this.resId = resId;
        this.path = path;
        this.maxFace = maxFace;
        this.effectType = effectType;
        this.description = description;
    }

    public String bundleName() {
        return bundleName;
    }

    public int resId() {
        return resId;
    }

    public String path() {
        return path;
    }

    public int maxFace() {
        return maxFace;
    }

    public int effectType() {
        return effectType;
    }

    public String description() {
        return description;
    }

    public Effect effect() {
        return new Effect(bundleName, resId, path, maxFace, effectType, description);
    }

    public static ArrayList<Effect> getEffectsByEffectType(int effectType) {
        ArrayList<Effect> effects = new ArrayList<>();
        effects.add(Effect_zhangxiaofan.effect());
        for (EffectEnum e : EffectEnum.values()) {
            if (e.effectType == effectType) {
                effects.add(e.effect());
            }
        }
        return effects;
    }

    public static ArrayList<Effect> getEffects() {
        ArrayList<Effect> effects = new ArrayList<>();
        for (EffectEnum e : EffectEnum.values()) {
            effects.add(e.effect());
        }
        return effects;
    }
}
