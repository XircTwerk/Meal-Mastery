package com.xirc.mealmastery.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.xirc.mealmastery.config.ConfigManager;
import com.xirc.mealmastery.config.ServerConfig;
import com.xirc.mealmastery.culinary.CulinaryClock;
import com.xirc.mealmastery.culinary.CulinaryProfile;
import com.xirc.mealmastery.culinary.MealRecord;
import com.xirc.mealmastery.culinary.ProfileManager;
import com.xirc.mealmastery.network.ServerPacketHandler;
import com.xirc.mealmastery.recipe.CulinaryRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Locale;

/**
 * Administrative subcommands.
 *
 * <p>Every one of these is gated on the configured admin permission level, and
 * the destructive ones require the scope to be named explicitly — there is no
 * bare "reset" that wipes a profile by accident.</p>
 */
public final class AdminCommands {

    private AdminCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("admin")
                .requires(source -> source.hasPermission(MealMasteryCommands.adminLevel()))
                .then(Commands.literal("inspect")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(AdminCommands::inspect)))
                .then(Commands.literal("addxp")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", LongArgumentType.longArg())
                                        .executes(AdminCommands::addXp))))
                .then(Commands.literal("setlevel")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                        .executes(AdminCommands::setLevel))))
                .then(Commands.literal("discover")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("dish", StringArgumentType.string())
                                        .executes(AdminCommands::discover))))
                .then(Commands.literal("mastery")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("dish", StringArgumentType.string())
                                        .then(Commands.argument("points", LongArgumentType.longArg(0))
                                                .executes(AdminCommands::setMastery)))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("scope", StringArgumentType.word())
                                        .executes(AdminCommands::reset))))
                .then(Commands.literal("purgeorphans")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(AdminCommands::purgeOrphans)));
    }

    private static int inspect(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        CommandSourceStack source = context.getSource();
        if (!source.hasPermission(ConfigManager.server().multiplayer.inspectPermissionLevel)) {
            source.sendFailure(Component.translatable("mealmastery.command.admin.no_permission"));
            return 0;
        }
        CulinaryProfile profile = ProfileManager.get().peekOffline(target.getUUID());
        ProfileReport.lines(profile, target.getGameProfile().getName())
                .forEach(line -> source.sendSuccess(() -> line, false));
        return 1;
    }

    private static int addXp(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        long amount = LongArgumentType.getLong(context, "amount");
        ServerConfig config = ConfigManager.server();

        CulinaryProfile profile = ProfileManager.get().of(target);
        if (amount >= 0L) {
            profile.addCookingXp(amount, config.progression.toCurve());
        } else {
            profile.setCookingXp(Math.max(0L, profile.cookingXp() + amount));
        }
        finish(context.getSource(), target, profile,
                "Cooking XP is now " + profile.cookingXp());
        return 1;
    }

    private static int setLevel(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        int level = IntegerArgumentType.getInteger(context, "level");
        ServerConfig config = ConfigManager.server();

        CulinaryProfile profile = ProfileManager.get().of(target);
        profile.setCookingXp(config.progression.toCurve().totalXpForLevel(level));
        finish(context.getSource(), target, profile, "Cooking level set to " + level);
        return 1;
    }

    private static int discover(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        ResourceLocation dish = parseDish(context);
        if (dish == null) {
            return 0;
        }
        CulinaryProfile profile = ProfileManager.get().of(target);
        boolean changed = profile.meal(dish).discover(CulinaryClock.day(target.server));
        profile.markDirty();
        finish(context.getSource(), target, profile,
                changed ? dish + " marked as discovered" : dish + " was already discovered");
        return 1;
    }

    private static int setMastery(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        ResourceLocation dish = parseDish(context);
        if (dish == null) {
            return 0;
        }
        long points = LongArgumentType.getLong(context, "points");
        CulinaryProfile profile = ProfileManager.get().of(target);

        // Set rather than add, so an admin correcting a value is not surprised
        // by it stacking on top of what was already there.
        MealRecord record = profile.meal(dish);
        record.setMasteryPoints(points, ConfigManager.server().mastery.toCurve());
        profile.markDirty();
        finish(context.getSource(), target, profile, dish + " mastery set to " + points);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        String rawScope = StringArgumentType.getString(context, "scope");
        CulinaryProfile.ResetScope scope;
        try {
            scope = CulinaryProfile.ResetScope.valueOf(rawScope.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException failure) {
            context.getSource().sendFailure(Component.translatable(
                    "mealmastery.command.admin.bad_scope",
                    List.of(CulinaryProfile.ResetScope.values()).toString()));
            return 0;
        }
        CulinaryProfile profile = ProfileManager.get().of(target);
        profile.reset(scope);
        finish(context.getSource(), target, profile, "Reset " + scope + " for "
                + target.getGameProfile().getName());
        return 1;
    }

    private static int purgeOrphans(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        CulinaryProfile profile = ProfileManager.get().of(target);
        int purged = profile.purgeOrphans(CulinaryRegistries.current().targets());
        finish(context.getSource(), target, profile,
                "Purged " + purged + " orphaned record(s)");
        return 1;
    }

    private static ResourceLocation parseDish(CommandContext<CommandSourceStack> context) {
        String raw = StringArgumentType.getString(context, "dish");
        ResourceLocation dish = ResourceLocation.tryParse(raw);
        if (dish == null || !BuiltInRegistries.ITEM.containsKey(dish)) {
            context.getSource().sendFailure(Component.translatable(
                    "mealmastery.command.admin.unknown_dish", raw));
            return null;
        }
        return dish;
    }

    /** Persists the change immediately and re-syncs the player's client. */
    private static void finish(CommandSourceStack source, ServerPlayer target,
                               CulinaryProfile profile, String message) {
        ProfileManager.get().flush(target.getUUID());
        ServerPacketHandler.sendProfile(target);
        source.sendSuccess(() -> Component.literal(message).withStyle(ChatFormatting.GREEN), true);
    }
}
