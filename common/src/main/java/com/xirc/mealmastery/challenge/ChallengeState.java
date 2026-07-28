package com.xirc.mealmastery.challenge;

import com.xirc.mealmastery.data.NbtUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * One player's progress through one challenge.
 *
 * <p>Objectives that count distinct subjects keep the subjects themselves, not
 * just a number, so "prepare 10 unique recipes you have never cooked" cannot be
 * satisfied by cooking the same dish ten times.</p>
 */
public final class ChallengeState {
    private final int[] counters;
    private final List<Set<ResourceLocation>> seen;
    private boolean completed;
    private long completedDay = Long.MIN_VALUE;

    public ChallengeState(int objectiveCount) {
        this.counters = new int[Math.max(1, objectiveCount)];
        this.seen = new ArrayList<>(counters.length);
        for (int i = 0; i < counters.length; i++) {
            seen.add(new LinkedHashSet<>());
        }
    }

    public boolean isCompleted() {
        return completed;
    }

    public long completedDay() {
        return completedDay;
    }

    public int progress(int objectiveIndex) {
        return objectiveIndex >= 0 && objectiveIndex < counters.length
                ? counters[objectiveIndex] : 0;
    }

    /** @return {@code true} when this call actually moved the objective forward */
    public boolean advance(int objectiveIndex, int amount) {
        if (completed || objectiveIndex < 0 || objectiveIndex >= counters.length || amount <= 0) {
            return false;
        }
        counters[objectiveIndex] += amount;
        return true;
    }

    /** @return {@code true} when {@code subject} had not been counted before */
    public boolean advanceUnique(int objectiveIndex, ResourceLocation subject) {
        if (completed || objectiveIndex < 0 || objectiveIndex >= counters.length
                || subject == null) {
            return false;
        }
        if (!seen.get(objectiveIndex).add(subject)) {
            return false;
        }
        counters[objectiveIndex] = seen.get(objectiveIndex).size();
        return true;
    }

    /** Used by objectives that track a best-so-far rather than a running total. */
    public boolean raise(int objectiveIndex, int value) {
        if (completed || objectiveIndex < 0 || objectiveIndex >= counters.length
                || value <= counters[objectiveIndex]) {
            return false;
        }
        counters[objectiveIndex] = value;
        return true;
    }

    public boolean satisfies(List<ChallengeObjective> objectives) {
        for (int i = 0; i < objectives.size() && i < counters.length; i++) {
            if (counters[i] < objectives.get(i).amount()) {
                return false;
            }
        }
        return objectives.size() <= counters.length;
    }

    public void complete(long day) {
        completed = true;
        completedDay = day;
    }

    public void reset() {
        java.util.Arrays.fill(counters, 0);
        seen.forEach(Set::clear);
        completed = false;
        completedDay = Long.MIN_VALUE;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("counters", counters.clone());
        NbtUtil.putIfTrue(tag, "completed", completed);
        if (completedDay != Long.MIN_VALUE) {
            tag.putLong("completedDay", completedDay);
        }
        boolean anySeen = seen.stream().anyMatch(set -> !set.isEmpty());
        if (anySeen) {
            ListTag list = new ListTag();
            for (Set<ResourceLocation> subjects : seen) {
                CompoundTag entry = new CompoundTag();
                entry.put("ids", NbtUtil.writeIds(subjects));
                list.add(entry);
            }
            tag.put("seen", list);
        }
        return tag;
    }

    public static ChallengeState load(CompoundTag tag, int objectiveCount) {
        int[] counters = tag.getIntArray("counters");
        ChallengeState state = new ChallengeState(Math.max(objectiveCount, counters.length));
        System.arraycopy(counters, 0, state.counters, 0,
                Math.min(counters.length, state.counters.length));
        state.completed = tag.getBoolean("completed");
        state.completedDay = tag.contains("completedDay") ? tag.getLong("completedDay")
                : Long.MIN_VALUE;
        if (tag.contains("seen", Tag.TAG_LIST)) {
            ListTag list = tag.getList("seen", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size() && i < state.seen.size(); i++) {
                final int index = i;
                NbtUtil.forEachId(list.getCompound(i), "ids",
                        id -> state.seen.get(index).add(id));
            }
        }
        return state;
    }
}
