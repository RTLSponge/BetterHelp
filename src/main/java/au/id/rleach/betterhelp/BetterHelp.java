package au.id.rleach.betterhelp;

import au.id.rleach.betterhelp.topics.Topic;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextParseException;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Optional;

@Plugin(id = au.id.rleach.betterhelp.Plugin.ID, name = au.id.rleach.betterhelp.Plugin.NAME, version = au.id.rleach.betterhelp.Plugin.VERSION)
public class BetterHelp {
    @Inject
    PluginContainer container;
    SpongeHelpCommand cmd;
    private CommandManager commandManager;

    @Inject
    @ConfigDir(sharedRoot = false)
    Path configDir;


    @Listener
    public void onStart(GameStartingServerEvent event){
        commandManager = Sponge.getCommandManager();
        cmd = new SpongeHelpCommand(this);
        commandManager.register(this, reload(), "helpReload");
        commandManager.register(this, cmd.create(), "helpTest");
        commandManager.register(this, dumpHelp(), "helpDump");
        commandManager.register(this, testTree(), "testTree");
        setup();
    }

    private CommandCallable testTree(){
        return CommandSpec.builder()
                .child(reload(), "reload")
                .child(cmd.create(), "test")
                .child(dumpHelp(), "dump")
                .build();
    }

    private void setup() {
        deRegister("help");
        cmd = new SpongeHelpCommand(this);
        commandManager.register(this, cmd.create(), "help");
        final String topics_conf = "topics.conf";
        CommentedConfigurationNode customTopics = ConfigLoader.loadConfigUnchecked(topics_conf, configDir, container);
    }

    private void printConfigNodes(CommentedConfigurationNode node, int depth, PrintStream ps){
        String indent = StringUtils.repeat("\t", depth);
        if(null == node) ps.println(indent+"null");
        final Object key = Optional.ofNullable(node.getKey()).orElse("null");
        final Object val = Optional.ofNullable(node.getValue()).orElse("null");
        node.getComment().ifPresent(
                comment -> ps.println(indent+"#"+comment)
        );
            ps.println(MessageFormat.format("{0}{1} -> {2}", indent, key, val));
        node.getChildrenMap().forEach(
                (key2,childNode) -> printConfigNodes(childNode,depth+1,ps)
        );
    }

    private CommandCallable reload(){
        return CommandSpec.builder().executor(
                (src, args) -> {
                    setup();
                    src.sendMessage(Text.of("Reloaded BetterHlep."));
                    return CommandResult.success();
                }
        ).build();
    }

    private Path dumpPath(){
        if(configDir == null) throw new Error("configDirectory was not set before calling dumpPath");
        return configDir.resolve("dump.conf");
    }

    private CommandCallable dumpHelp(){
        return CommandSpec.builder().executor(
                (src, args) -> {
                    HoconConfigurationLoader.Builder builder = HoconConfigurationLoader.builder().setPath(dumpPath());
                    TypeSerializerCollection mySerialization = builder.getDefaultOptions().getSerializers().newChild();
                    TypeSerializer<Text> xmlSerializer = new xmlTypeSerializer();
                    mySerialization.registerType(TypeToken.of(Text.class), xmlSerializer);
                    HoconConfigurationLoader loader = builder.build();
                    CommentedConfigurationNode x = dumpHelp(src, args);
                    try {
                        loader.save(x);
                    } catch (IOException e) {
                        throw new CommandException(Text.of(e.toString()));
                    }
                    return CommandResult.success();
                }
            )
        .arguments(GenericArguments.string(Text.of("topic")))
        .build();
    }

    private CommentedConfigurationNode dumpHelp(CommandSource src, CommandContext args) throws CommandException {
        Optional<String> topic = args.getOne("topic");
        Topic.CommandTopicBuilder builder = new Topic.CommandTopicBuilder();
        Topic root = builder.fillRootTopic(Sponge.getCommandManager(), cmd.createPermissionDescriptionFactory());
        if(!topic.isPresent()) {
            throw new CommandException(Text.of("topic is not optional"));
        }
        Optional<Topic> found = root.search(topic.get());
        if(!found.isPresent()) {
            throw new CommandException(Text.of("topic was not found"));
        }else {
            try {
                return found.get().toNode();
            } catch (ObjectMappingException e) {
                throw new CommandException(Text.of(e));
            }
        }
    }

    private void deRegister(String command){
        Optional<? extends CommandMapping> helpMapping = Sponge.getCommandManager().get(command);
        helpMapping.ifPresent(mapping->
                Sponge.getCommandManager().removeMapping(mapping)
        );

    }

    private static class xmlTypeSerializer implements TypeSerializer<Text> {

        xmlTypeSerializer() {
        }

        @Override public void serialize(TypeToken<?> type, Text obj, ConfigurationNode value) {
            TextSerializers.TEXT_XML.serialize(obj);
        }

        @Override public Text deserialize(TypeToken type, ConfigurationNode value) throws ObjectMappingException {
            try {
                return TextSerializers.TEXT_XML.deserialize(value.getString());
            } catch (TextParseException e) {
                throw new ObjectMappingException(e);
            }
        }
    }


}


