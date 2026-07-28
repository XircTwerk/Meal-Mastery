package com.xirc.mealmastery.recipe;

import com.xirc.mealmastery.MealMasteryLog;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The cached view of every dish this server knows how to make.
 *
 * <p>Rebuilt once per recipe reload and never touched again, which is what
 * keeps the design's performance rules satisfiable: nothing scans the
 * recipe registry outside a reload, and every journal query is a map lookup
 * against these indexes.</p>
 *
 * <p>Instances are immutable. Player progress is deliberately <em>not</em> here:
 * removing an addon rebuilds this registry without its dishes while the
 * profiles keep their records intact.</p>
 */
public final class CulinaryRegistry {

    public static final CulinaryRegistry EMPTY = new CulinaryRegistry(
            Map.of(), List.of(), CulinaryAudit.EMPTY);

    private final Map<ResourceLocation, MealEntry> byTarget;
    private final List<MealEntry> ordered;
    private final CulinaryAudit audit;

    private final Map<CookingMethod, List<MealEntry>> byMethod;
    private final Map<String, List<MealEntry>> byMod;
    private final Map<ResourceLocation, List<MealEntry>> byCategory;
    private final Map<ResourceLocation, List<MealEntry>> byIngredient;
    private final Set<ResourceLocation> allIngredients;

    private CulinaryRegistry(Map<ResourceLocation, MealEntry> byTarget,
                             List<MealEntry> ordered,
                             CulinaryAudit audit) {
        this.byTarget = Map.copyOf(byTarget);
        this.ordered = List.copyOf(ordered);
        this.audit = audit;

        Map<CookingMethod, List<MealEntry>> methods = new LinkedHashMap<>();
        Map<String, List<MealEntry>> mods = new LinkedHashMap<>();
        Map<ResourceLocation, List<MealEntry>> categories = new LinkedHashMap<>();
        Map<ResourceLocation, List<MealEntry>> ingredients = new LinkedHashMap<>();
        Set<ResourceLocation> ingredientIds = new LinkedHashSet<>();

        for (MealEntry entry : this.ordered) {
            for (CookingMethod method : entry.methods()) {
                methods.computeIfAbsent(method, unused -> new ArrayList<>()).add(entry);
            }
            mods.computeIfAbsent(entry.sourceModId(), unused -> new ArrayList<>()).add(entry);
            categories.computeIfAbsent(entry.category().id(), unused -> new ArrayList<>()).add(entry);
            for (ResourceLocation ingredient : entry.ingredients()) {
                ingredients.computeIfAbsent(ingredient, unused -> new ArrayList<>()).add(entry);
                ingredientIds.add(ingredient);
            }
        }

        this.byMethod = freeze(methods);
        this.byMod = freeze(mods);
        this.byCategory = freeze(categories);
        this.byIngredient = freeze(ingredients);
        this.allIngredients = Set.copyOf(ingredientIds);
    }

    private static <K> Map<K, List<MealEntry>> freeze(Map<K, List<MealEntry>> source) {
        Map<K, List<MealEntry>> frozen = new LinkedHashMap<>(source.size());
        source.forEach((key, value) -> frozen.put(key, List.copyOf(value)));
        return Collections.unmodifiableMap(frozen);
    }

    // --------------------------------------------------------------- queries

    public MealEntry entry(ResourceLocation target) {
        return byTarget.get(target);
    }

    public boolean isTracked(ResourceLocation target) {
        return byTarget.containsKey(target);
    }

    public Set<ResourceLocation> targets() {
        return byTarget.keySet();
    }

    public List<MealEntry> entries() {
        return ordered;
    }

    public int size() {
        return ordered.size();
    }

    public CulinaryAudit audit() {
        return audit;
    }

    public List<MealEntry> byMethod(CookingMethod method) {
        return byMethod.getOrDefault(method, List.of());
    }

    public Set<CookingMethod> methods() {
        return byMethod.keySet();
    }

    public List<MealEntry> byMod(String modId) {
        return byMod.getOrDefault(modId, List.of());
    }

    public Set<String> sourceMods() {
        return byMod.keySet();
    }

    public List<MealEntry> byCategory(ResourceLocation categoryId) {
        return byCategory.getOrDefault(categoryId, List.of());
    }

    public Set<ResourceLocation> categories() {
        return byCategory.keySet();
    }

    public List<MealEntry> usingIngredient(ResourceLocation ingredient) {
        return byIngredient.getOrDefault(ingredient, List.of());
    }

    /** Every ingredient that appears in at least one tracked recipe. */
    public Set<ResourceLocation> allIngredients() {
        return allIngredients;
    }

    // ----------------------------------------------------------------- build

    /**
     * Classifies every recipe on the server into journal entries.
     *
     * @param recipes        the full recipe list, straight from the recipe manager
     * @param registryAccess needed to resolve recipe results
     * @param rules          eligibility configuration
     * @param dishCategories category definitions, most specific first
     */
    public static CulinaryRegistry build(Collection<? extends Recipe<?>> recipes,
                                         RegistryAccess registryAccess,
                                         EligibilityRules rules,
                                         List<FoodCategory> dishCategories) {
        long startedAt = System.nanoTime();
        RecipeClassifier classifier = new RecipeClassifier(rules, registryAccess);

        Map<ResourceLocation, List<RecipeEntry>> grouped = new LinkedHashMap<>();
        Map<CookingMethod, Integer> perMethod = new LinkedHashMap<>();
        Map<EligibilityRules.Verdict, Integer> verdictCounts = new EnumMap<>(EligibilityRules.Verdict.class);
        int failed = 0;

        for (Recipe<?> recipe : recipes) {
            RecipeClassifier.Classification classification = classifier.classify(recipe);
            if (classification.failed()) {
                failed++;
                continue;
            }
            verdictCounts.merge(classification.verdict(), 1, Integer::sum);
            RecipeEntry entry = classification.entry();
            if (entry == null) {
                continue;
            }
            grouped.computeIfAbsent(entry.masteryTarget(), unused -> new ArrayList<>()).add(entry);
            perMethod.merge(entry.method(), 1, Integer::sum);
        }

        List<MealEntry> ordered = new ArrayList<>(grouped.size());
        List<ResourceLocation> uncategorised = new ArrayList<>();
        Set<String> sourceMods = new LinkedHashSet<>();

        grouped.forEach((target, entries) -> {
            Item item = BuiltInRegistries.ITEM.get(target);
            if (item == null) {
                return;
            }
            Set<CookingMethod> methods = new LinkedHashSet<>();
            Set<ResourceLocation> ingredients = new LinkedHashSet<>();
            for (RecipeEntry entry : entries) {
                methods.add(entry.method());
                for (RecipeEntry.IngredientSlot slot : entry.ingredients()) {
                    ingredients.addAll(slot.items());
                }
            }

            ItemStack stack = new ItemStack(item);
            FoodProperties food = item.getFoodProperties();

            FoodCategory category = categorise(stack, dishCategories);
            if (category == FoodCategory.UNCATEGORISED) {
                uncategorised.add(target);
                // Edible but untagged is the common case, not an error.
                category = food == null ? FoodCategory.UNCATEGORISED : FoodCategory.OTHER_FOOD;
            }
            sourceMods.add(target.getNamespace());

            ordered.add(new MealEntry(
                    target,
                    item,
                    target.getNamespace(),
                    entries,
                    methods,
                    ingredients,
                    category,
                    food == null ? 0 : food.getNutrition(),
                    food == null ? 0.0F : food.getSaturationModifier()));
        });

        // Alphabetical by id keeps the journal, the audit and the exported
        // profile listing every dish in the same stable order.
        ordered.sort((left, right) -> left.target().compareTo(right.target()));

        Map<ResourceLocation, MealEntry> byTarget = new LinkedHashMap<>(ordered.size());
        for (MealEntry entry : ordered) {
            byTarget.put(entry.target(), entry);
        }

        CulinaryAudit audit = new CulinaryAudit(
                ordered.size(),
                verdictCounts.getOrDefault(EligibilityRules.Verdict.ELIGIBLE, 0),
                verdictCounts.getOrDefault(EligibilityRules.Verdict.NOT_FOOD, 0),
                verdictCounts.getOrDefault(EligibilityRules.Verdict.EXCLUDED, 0),
                failed,
                perMethod,
                sourceMods,
                uncategorised);

        MealMasteryLog.LOGGER.info(
                "Culinary registry rebuilt: {} dishes from {} eligible recipes across {} mods "
                        + "({} methods, {} uncategorised) in {} ms",
                ordered.size(), audit.eligibleRecipes(), sourceMods.size(),
                perMethod.size(), uncategorised.size(),
                (System.nanoTime() - startedAt) / 1_000_000L);

        return new CulinaryRegistry(byTarget, ordered, audit);
    }

    private static FoodCategory categorise(ItemStack stack, List<FoodCategory> definitions) {
        for (FoodCategory category : definitions) {
            if (category.matches(stack)) {
                return category;
            }
        }
        return FoodCategory.UNCATEGORISED;
    }
}
