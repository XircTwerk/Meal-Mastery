package com.xirc.mealmastery.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.xirc.mealmastery.Constants;
import com.xirc.mealmastery.MealMasteryServer;
import com.xirc.mealmastery.challenge.ChallengeDefinition;
import com.xirc.mealmastery.challenge.ChallengeRegistry;
import com.xirc.mealmastery.challenge.ChallengeState;
import com.xirc.mealmastery.challenge.DailyRotation;
import com.xirc.mealmastery.compat.CompatibilityReport;
import com.xirc.mealmastery.collection.CollectionDefinition;
import com.xirc.mealmastery.collection.CollectionRegistry;
import com.xirc.mealmastery.config.ConfigManager;
import com.xirc.mealmastery.config.ServerConfig;
import com.xirc.mealmastery.culinary.CulinaryProfile;
import com.xirc.mealmastery.culinary.CulinaryTitle;
import com.xirc.mealmastery.culinary.MealRecord;
import com.xirc.mealmastery.culinary.ProfileManager;
import com.xirc.mealmastery.multiplayer.Leaderboards;
import com.xirc.mealmastery.network.Network;
import com.xirc.mealmastery.network.Packets;
import com.xirc.mealmastery.recipe.CookingMethod;
import com.xirc.mealmastery.recipe.CulinaryAudit;
import com.xirc.mealmastery.recipe.CulinaryRegistries;
import com.xirc.mealmastery.recipe.CulinaryRegistry;
import com.xirc.mealmastery.recipe.MealEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * The player-facing command tree.
 *
 * <p>Registered under {@code /mealmastery} with {@code /mealmasters} and
 * {@code /mm} as aliases. Administrative subcommands live in
 * {@link AdminCommands} and are permission-gated there.</p>
 */
public final class MealMasteryCommands {

    private MealMasteryCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                CommandBuildContext context) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(Constants.COMMAND)
                .executes(source -> openJournal(source.getSource()))
                .then(Commands.literal("open")
                        .executes(source -> openJournal(source.getSource())))
                .then(Commands.literal("stats")
                        .executes(source -> stats(source.getSource())))
                .then(Commands.literal("recipe")
                        .then(Commands.argument("item", ItemArgument.item(context))
                                .executes(source -> recipe(source.getSource(),
                                        ItemArgument.getItem(source, "item").createItemStack(1, false)))))
                .then(Commands.literal("challenges")
                        .executes(source -> challenges(source.getSource())))
                .then(Commands.literal("collections")
                        .executes(source -> collections(source.getSource())))
                .then(Commands.literal("titles")
                        .executes(source -> titles(source.getSource())))
                .then(Commands.literal("leaderboard")
                        .executes(source -> leaderboard(source.getSource())))
                .then(Commands.literal("compat")
                        .executes(source -> compatibility(source.getSource())))
                .then(Commands.literal("today")
                        .executes(source -> today(source.getSource())))
                .then(Commands.literal("export")
                        .executes(source -> export(source.getSource())))
                .then(Commands.literal("audit")
                        .requires(source -> source.hasPermission(adminLevel()))
                        .executes(source -> audit(source.getSource())))
                .then(Commands.literal("debug")
                        .requires(source -> source.hasPermission(adminLevel()))
                        .executes(source -> debug(source.getSource())))
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(adminLevel()))
                        .executes(source -> reload(source.getSource())))
                .then(AdminCommands.build());

        dispatcher.register(root);
        dispatcher.register(Commands.literal(Constants.COMMAND_ALIAS_LONG)
                .redirect(dispatcher.getRoot().getChild(Constants.COMMAND)));
        dispatcher.register(Commands.literal(Constants.COMMAND_ALIAS_SHORT)
                .redirect(dispatcher.getRoot().getChild(Constants.COMMAND)));
    }

    static int adminLevel() {
        return ConfigManager.server().multiplayer.adminPermissionLevel;
    }

    // ------------------------------------------------------------- commands

    private static int openJournal(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Network.toPlayer(player, new Packets.OpenJournal());
        return 1;
    }

    private static int stats(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CulinaryProfile profile = ProfileManager.get().of(player);
        ProfileReport.lines(profile, player.getGameProfile().getName())
                .forEach(line -> source.sendSuccess(() -> line, false));
        return 1;
    }

    private static int recipe(CommandSourceStack source, ItemStack stack)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ResourceLocation target = BuiltInRegistries.ITEM.getKey(stack.getItem());
        MealEntry entry = CulinaryRegistries.current().entry(target);
        if (entry == null) {
            source.sendFailure(Component.translatable("mealmastery.command.recipe.unknown",
                    stack.getHoverName()));
            return 0;
        }
        CulinaryProfile profile = ProfileManager.get().of(player);
        ProfileReport.recipeLines(entry, profile).forEach(line -> source.sendSuccess(() -> line, false));
        return 1;
    }

    private static int challenges(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CulinaryProfile profile = ProfileManager.get().of(player);
        ChallengeRegistry registry = ChallengeRegistry.current();

        if (registry.size() == 0) {
            source.sendSuccess(() -> Component.translatable("mealmastery.command.challenges.none")
                    .withStyle(ChatFormatting.GRAY), false);
            return 1;
        }
        source.sendSuccess(() -> header("mealmastery.command.challenges.title"), false);
        for (ChallengeDefinition definition : registry.all()) {
            if (definition.hidden() && !profile.hasCompletedChallenge(definition.id())) {
                continue;
            }
            ChallengeState state = profile.peekChallenge(definition.id());
            boolean done = state != null && state.isCompleted();
            int progress = state == null ? 0 : state.progress(0);
            int goal = definition.objectives().get(0).amount();
            source.sendSuccess(() -> Component.literal(done ? "  ✔ " : "  • ")
                    .withStyle(done ? ChatFormatting.GREEN : ChatFormatting.GRAY)
                    .append(Component.translatable(definition.titleKey())
                            .withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(done ? "" : "  " + progress + " / " + goal)
                            .withStyle(ChatFormatting.DARK_GRAY)), false);
        }
        return 1;
    }

    private static int collections(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CulinaryProfile profile = ProfileManager.get().of(player);
        CulinaryRegistry culinary = CulinaryRegistries.current();
        ServerConfig config = ConfigManager.server();

        source.sendSuccess(() -> header("mealmastery.command.collections.title"), false);
        for (CollectionDefinition definition : CollectionRegistry.current().defined()) {
            CollectionDefinition.Progress progress =
                    definition.progress(culinary, profile, config.mastery.toCurve());
            source.sendSuccess(() -> Component.literal("  ")
                    .append(Component.translatable(definition.titleKey())
                            .withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("  " + progress.discovered() + " / " + progress.total())
                            .withStyle(ChatFormatting.GRAY)), false);
        }
        // Per-mod collections are generated, so they are listed separately.
        for (CollectionRegistry.ModCollection mod : CollectionRegistry.current().modCollections()) {
            int discovered = 0;
            for (ResourceLocation member : mod.members()) {
                if (profile.isDiscovered(member)) {
                    discovered++;
                }
            }
            final int found = discovered;
            source.sendSuccess(() -> Component.literal("  " + mod.displayName())
                    .withStyle(ChatFormatting.WHITE)
                    .append(Component.literal("  " + found + " / " + mod.members().size())
                            .withStyle(ChatFormatting.GRAY)), false);
        }
        return 1;
    }

    private static int titles(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CulinaryProfile profile = ProfileManager.get().of(player);
        ServerConfig config = ConfigManager.server();
        List<CulinaryTitle> unlocked = CulinaryTitle.unlockedFor(profile,
                config.progression.toCurve(), config.mastery.toCurve());

        source.sendSuccess(() -> header("mealmastery.command.titles.title"), false);
        if (unlocked.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("mealmastery.command.titles.none")
                    .withStyle(ChatFormatting.GRAY), false);
            return 1;
        }
        unlocked.forEach(title -> source.sendSuccess(() -> Component.literal("  ")
                .append(Component.translatable(title.translationKey())
                        .withStyle(ChatFormatting.GOLD)), false));
        return 1;
    }

    private static int export(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CulinaryProfile profile = ProfileManager.get().of(player);
        // Plain text rather than a file: readable in chat, copyable, and it
        // cannot write anywhere the player did not ask for.
        ProfileReport.exportLines(profile, player.getGameProfile().getName())
                .forEach(line -> source.sendSuccess(() -> line, false));
        return 1;
    }

    private static int leaderboard(CommandSourceStack source) {
        if (!Leaderboards.enabled()) {
            source.sendFailure(Component.translatable("mealmastery.command.leaderboard.disabled"));
            return 0;
        }
        source.sendSuccess(() -> header("mealmastery.command.leaderboard.title"), false);
        for (Leaderboards.Category category : Leaderboards.Category.values()) {
            List<Leaderboards.Entry> entries = Leaderboards.top(source.getServer(), category);
            if (entries.isEmpty()) {
                continue;
            }
            source.sendSuccess(() -> Component.literal("  " + category.name())
                    .withStyle(ChatFormatting.GRAY), false);
            int rank = 1;
            for (Leaderboards.Entry entry : entries) {
                final int position = rank++;
                source.sendSuccess(() -> Component.literal(
                                "    " + position + ". " + entry.name() + " — " + entry.value())
                        .withStyle(ChatFormatting.WHITE), false);
            }
        }
        return 1;
    }

    private static int compatibility(CommandSourceStack source) {
        source.sendSuccess(() -> header("mealmastery.command.compat.title"), false);
        for (CompatibilityReport.Line line : CompatibilityReport.build()) {
            source.sendSuccess(() -> Component.literal("  " + line.displayName())
                    .withStyle(ChatFormatting.WHITE)
                    .append(Component.literal("  "))
                    .append(Component.translatable(line.status().translationKey())
                            .withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(line.dishCount() > 0
                                    ? "  (" + line.dishCount() + " dishes)" : "")
                            .withStyle(ChatFormatting.DARK_GRAY)), false);
        }
        return 1;
    }

    private static int today(CommandSourceStack source) {
        ResourceLocation dish = DailyRotation.recipeOfTheDay();
        if (dish == null) {
            source.sendFailure(Component.translatable("mealmastery.command.today.none"));
            return 0;
        }
        source.sendSuccess(() -> header("mealmastery.command.today.title"), false);
        MealEntry entry = CulinaryRegistries.current().entry(dish);
        source.sendSuccess(() -> Component.literal("  ")
                .append(entry == null ? Component.literal(dish.toString())
                        : Component.translatable(entry.translationKey()))
                .withStyle(ChatFormatting.WHITE), false);
        for (var challenge : DailyRotation.dailyChallenges()) {
            source.sendSuccess(() -> Component.literal("  • ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.translatable(
                                    challenge.objectives().get(0).translationKey())
                            .append(" x" + challenge.objectives().get(0).amount()))
                    .withStyle(ChatFormatting.WHITE), false);
        }
        return 1;
    }

    private static int audit(CommandSourceStack source) {
        CulinaryRegistry registry = CulinaryRegistries.current();
        CulinaryAudit audit = registry.audit();

        source.sendSuccess(() -> header("mealmastery.command.audit.title"), false);
        line(source, "Detected food items", audit.detectedFoodItems());
        line(source, "Eligible recipes", audit.eligibleRecipes());
        line(source, "Ignored recipes (no edible output)", audit.ignoredRecipes());
        line(source, "Excluded by configuration", audit.excludedRecipes());
        line(source, "Failed inspection", audit.failedRecipes());
        line(source, "Source mods", audit.sourceMods().size());
        line(source, "Unclassified dishes", audit.uncategorised().size());

        for (Map.Entry<CookingMethod, Integer> entry : audit.recipesPerMethod().entrySet()) {
            source.sendSuccess(() -> Component.literal("  " + entry.getKey().id() + ": ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.valueOf(entry.getValue()))
                            .withStyle(ChatFormatting.WHITE)), false);
        }
        if (!audit.uncategorised().isEmpty()) {
            source.sendSuccess(() -> Component.literal("  Uncategorised: "
                            + audit.uncategorised().stream().limit(10)
                            .map(ResourceLocation::toString).toList())
                    .withStyle(ChatFormatting.DARK_GRAY), false);
        }
        return 1;
    }

    /**
     * Reports what Meal Mastery thinks about the held item. This is the
     * fastest way to find out why an addon's food is or is not in the journal.
     */
    private static int debug(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        // Reported before the held-item checks so it is available even with an
        // empty hand - this is what tells you whether the screen you have open
        // was recognised as a cooking surface.
        source.sendSuccess(() -> header("mealmastery.command.debug.title"), false);
        text(source, "Open menu", player.containerMenu == null
                ? "none" : player.containerMenu.getClass().getSimpleName());
        text(source, "Tracking",
                com.xirc.mealmastery.tracking.CookingTracker.describe(
                        player, player.server.getTickCount()));
        text(source, "Detected workstations",
                String.valueOf(com.xirc.mealmastery.tracking.WorkstationRegistry.all().size()));

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("mealmastery.command.debug.empty")
                    .withStyle(ChatFormatting.DARK_GRAY), false);
            return 1;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(held.getItem());
        MealEntry entry = CulinaryRegistries.current().entry(itemId);

        text(source, "Held item", String.valueOf(itemId));
        text(source, "Edible", String.valueOf(held.isEdible()));
        text(source, "Tracked", String.valueOf(entry != null));
        if (entry == null) {
            return 1;
        }
        text(source, "Source", entry.sourceModId());
        text(source, "Category", entry.category().id().toString());
        text(source, "Primary method", entry.primaryMethod().id().toString());
        text(source, "Detected recipes", String.valueOf(entry.recipes().size()));
        text(source, "Distinct ingredients", String.valueOf(entry.ingredients().size()));
        text(source, "Nutrition / saturation",
                entry.nutrition() + " / " + entry.saturation());

        MealRecord record = ProfileManager.get().of(player).peekMeal(itemId);
        text(source, "Your record", record == null ? "none"
                : record.prepared() + " prepared, " + record.masteryPoints() + " mastery points");
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        MealMasteryServer.reload();
        source.sendSuccess(() -> Component.translatable("mealmastery.command.reload.done",
                CulinaryRegistries.current().size()).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    // --------------------------------------------------------------- output

    static Component header(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    }

    private static void line(CommandSourceStack source, String label, int value) {
        text(source, label, String.valueOf(value));
    }

    private static void text(CommandSourceStack source, String label, String value) {
        source.sendSuccess(() -> Component.literal("  " + label + ": ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(ChatFormatting.WHITE)), false);
    }
}
