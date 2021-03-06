package org.androidtown.homecare.Models;

/**
 * Created by hanhb on 2017-11-16.
 */

public class Estimation {

    private String comment, key, uid, name;
    private Double kindness, wellness, faithfulness; //친절함, 업무숙련도, 성실함

    public Estimation() {

    }

    public Estimation(String key, String comment, Double kindness, Double wellness, Double faithfulness, String uid, String name) {
        this.comment = comment;
        this.kindness = kindness;
        this.wellness = wellness;
        this.faithfulness = faithfulness;
        this.key = key;
        this.uid = uid;
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Double getKindness() {
        return kindness;
    }

    public void setKindness(Double kindness) {
        this.kindness = kindness;
    }

    public Double getWellness() {
        return wellness;
    }

    public void setWellness(Double wellness) {
        this.wellness = wellness;
    }

    public Double getFaithfulness() {
        return faithfulness;
    }

    public void setFaithfulness(Double faithfulness) {
        this.faithfulness = faithfulness;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
