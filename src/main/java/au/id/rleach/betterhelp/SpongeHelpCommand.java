package au.id.rleach.betterhelp;/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import static org.spongepowered.api.command.args.GenericArguments.optional;
import static org.spongepowered.api.command.args.GenericArguments.string;

import au.id.rleach.betterhelp.topics.Topic;
import com.google.common.collect.Collections2;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

public class SpongeHelpCommand {
    private static final Comparator<CommandMapping> COMMAND_COMPARATOR = (o1, o2) -> o1.getPrimaryAlias().compareTo(o2.getPrimaryAlias());
    private final BetterHelp plugin;

    SpongeHelpCommand(BetterHelp betterHelp){
        this.plugin = betterHelp;
    }

    public CommandSpec create() {
        return CommandSpec
                .builder()
                .arguments(optional(string(Text.of("topic"))))
                .description(Text.of("View a list of all commands."))
                .extendedDescription(
                        Text.of("View a list of all commands. Hover over\n" + " a command to view its description. Click\n"
                                + " a command to insert it into your chat bar."))
                .executor((src, args) -> {
                    Supplier<PermissionDescription.Builder> factory = createPermissionDescriptionFactory();
                    Topic.CommandTopicBuilder tBuilder = new Topic.CommandTopicBuilder();
                    Topic root = tBuilder.fillRootTopic(Sponge.getGame().getCommandManager(), factory);
                    PaginationList.Builder builder;
                    Optional<String> sTopic = args.getOne("topic");
                    if (sTopic.isPresent()) {
                        Optional<Topic> optTopic = root.search(sTopic.get());
                        if (optTopic.isPresent()) {
                             builder = createPagination(optTopic.get(), src);
                            builder.sendTo(src);
                            return CommandResult.success();
                        }
                        throw new CommandException(Text.of("No such topic/command: ", sTopic.get()));
                    }
                    builder = createPagination(root, src);
                    builder.sendTo(src);
                    return CommandResult.success();
                }).build();
    }

    private PaginationList.Builder createPagination(Topic topic, CommandSource src) {
        PaginationList.Builder builder = Sponge.getGame().getServiceManager().provide(PaginationService.class).get().builder();
        createPermissionDescriptionFactory();
        builder.title(topic.getTitle()).contents(topic.toTexts(false));
        return builder;
    }

    Supplier<PermissionDescription.Builder> createPermissionDescriptionFactory() {
        Optional<PermissionService> permissionService = Sponge.getServiceManager().provide(PermissionService.class);
        PermissionDescription.Builder mockBuilder = new PermissionDescription.Builder(){

            @Override public PermissionDescription.Builder id(String permissionId) {
                return this;
            }

            @Override public PermissionDescription.Builder description(Text description) {
                return this;
            }

            @Override public PermissionDescription.Builder assign(String role, boolean value) {
                return this;
            }

            @Override public PermissionDescription register() throws IllegalStateException {
                return new PermissionDescription(){
                    @Override public String getId() {
                        return "";
                    }

                    @Override public Text getDescription() {
                        return Text.EMPTY;
                    }

                    @Override public Map<Subject, Boolean> getAssignedSubjects(String type) {
                        return Collections.EMPTY_MAP;
                    }

                    @Override public PluginContainer getOwner() {
                        return Sponge.getPluginManager().fromInstance(plugin).get();
                    }
                };
            }
        };
        Supplier<PermissionDescription.Builder> factory = () -> {
          if (permissionService.isPresent()) {
              Optional<PermissionDescription.Builder> pdBuilder = permissionService.get().newDescriptionBuilder(plugin);
              if (pdBuilder.isPresent()) {
                  return pdBuilder.get();
              }
          }
            return mockBuilder;
        };
        return factory;
    }

    private Set<CommandMapping> getCommandMappings(CommandSource src) {
        Set<CommandMapping> commands = new TreeSet<>(COMMAND_COMPARATOR);
        commands.addAll(Collections2.filter(Sponge.getGame().getCommandManager().getAll().values(), input -> input.getCallable()
                .testPermission(src)));
        return commands;
    }

    private static Text getDescription(CommandSource source, CommandMapping mapping) {
        @SuppressWarnings("unchecked")
        final Optional<Text> description = (Optional<Text>) mapping.getCallable().getShortDescription(source);
        Text.Builder text = Text.builder("/" + mapping.getPrimaryAlias());
        text.color(TextColors.GREEN);
        //text.style(TextStyles.UNDERLINE);
        text.onClick(TextActions.suggestCommand("/" + mapping.getPrimaryAlias()));
        Optional<? extends Text> longDescription = mapping.getCallable().getHelp(source);
        if (longDescription.isPresent()) {
            text.onHover(TextActions.showText(longDescription.get()));
        }
        return Text.of(text, " ", description.orElse(mapping.getCallable().getUsage(source)));
    }

}
