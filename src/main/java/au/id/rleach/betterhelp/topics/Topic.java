package au.id.rleach.betterhelp.topics;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandManager;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextTemplate;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nullable;


public final class Topic {

    private static final int MAX_LINES = 20;
    private final Text title; // WorldEdit
    private final Text shortDescription; // List the WorldEdit commands & help pages
    private final Text longDescription; //List of world edit commands / long docs.
    private final PermissionDescription permission;
    private final Deque<Topic> subTopics;
    final String key;
    @Nullable private final Topic parent;
    final int priority;
    public int depth;

    private Topic(
            final String key,
            @Nullable final Topic parent,
            final Text shortDescription,
            final Text longDescription,
            final PermissionDescription.Builder pdBuilder,
            final String permission,
            final TextTemplate shortTemplate,
            final TextTemplate longTemplate,
            final int priority
    ) {
        this.key = key;
        this.shortDescription = shortDescription;
        this.longDescription = longDescription;
        pdBuilder.id(permission);
        pdBuilder.description(Text.of("User can see help for topic : " + key));
        pdBuilder.assign(PermissionDescription.ROLE_USER, true);
        this.permission = pdBuilder.register();
        subTopics = Lists.newLinkedList();
        this.parent = parent;
        title = getTitleText();
        this.priority = priority;
    }

    private Text getTitleText(){
        if (null == parent) {
            return anchor(key,()->toText(MAX_LINES), false);
        } else {
            return Text.joinWith(Text.of(">"), parent.getTitleText(), anchor(key,()->toText(MAX_LINES), false));
        }
    }

    private static Text anchor(final String key, final Supplier<Text> lazyHoverText, final boolean nested){
        if(nested) {
            return Text.of(
                    TextColors.GREEN, TextStyles.UNDERLINE,
                    key
            );
        } else {
            final String command = getHelpCommand(key);
            return Text.of(
                    TextColors.GREEN, TextStyles.UNDERLINE,
                    TextActions.insertText(command),
                    TextActions.runCommand(command),
                    TextActions.showText(lazyHoverText.get()),
                    key
            );
        }
    }

    private static String getHelpCommand(final String key) {
        final String command;
        if(isRootKey(key)){
            command = "/help"; //TODO: use registered alias instead.
        } else {
            command = "/help \"" + key + '"';
        }
        return command;
    }

    private static boolean isRootKey(final String key) {
        //TODO: refactor to take a topic
        return "help".equals(key);
    }

    public Text getTitle() {
        return title;
    }

    public Text[] toTexts(final boolean nested) {
        final List<Text> texts = Lists.newArrayList();
        texts.add(Text.of(key));
        texts.add(Text.of(TextColors.GRAY,"  ",shortDescription));
        texts.add(Text.of("  ",longDescription));
        for(final Topic t : subTopics){
            texts.add(t.toSmallText(nested));
        }
        final Text[] ret = new Text[texts.size()];
        return texts.toArray(ret);
    }

    private Text toText(int maxLines) {
        final Text[] texts = toTexts(true);
        String continuation = "\n...";
        if(maxLines < 0 || maxLines > texts.length) {
            maxLines = texts.length-1;
            continuation = "";
        }
        return Text.of(Text.joinWith(Text.of("\n"), Arrays.copyOfRange(texts,0,maxLines)), continuation);
    }

    private Text toSmallText(final boolean nested) {
        return Text.of(
                Text.of(
                        TextColors.GREEN,
                        TextStyles.UNDERLINE,
                        anchor(key,()->toText(20), nested)
                )
                ," ",
                Text.of(
                        TextColors.GRAY,
                        shortDescription
                ),
                Text.of("smalltext")
        );
    }

    public Optional<Topic> search(final String sTopic) {
        if(key.equalsIgnoreCase(sTopic)) {
            return Optional.of(this);
        } else {
            for (final Topic t:subTopics) {
                final Optional<Topic> ret = t.search(sTopic);
                if(ret.isPresent()) {
                    return ret;
                }
            }
            return Optional.empty();
        }
    }

    public CommentedConfigurationNode toNode() throws ObjectMappingException {
        final SimpleCommentedConfigurationNode node = SimpleCommentedConfigurationNode.root();
        node.setComment("Dumped: " + getPath());
        node.getNode((Object[]) this.getPath().split("\\.")).setValue(toNodeInternal(SimpleCommentedConfigurationNode.root()));
        return node;
    }

    private ConfigurationNode toNodeInternal(final ConfigurationNode node) throws ObjectMappingException {

        node.getNode(Topic.TopicConfigSerializer.SHORT_KEY).setValue(TypeToken.of(Text.class), shortDescription);
        node.getNode(Topic.TopicConfigSerializer.LONG_KEY).setValue(TypeToken.of(Text.class), longDescription);
        for(final Topic child:subTopics){
            node.getNode(Topic.TopicConfigSerializer.SUBTOPICS_KEY, child.key).setValue(child.toNodeInternal(SimpleCommentedConfigurationNode.root()));
        }
        return node;

    }

    private String getPath(){
        if(null == parent) {
            return this.key;
        }
        return Joiner.on(".").join(parent.getPath(), this.key);
    }

    public static class CommandTopicBuilder {

        private final TextTemplate rootTemplateShort = TextTemplate.EMPTY;
        private final TextTemplate rootTemplateLong = TextTemplate.EMPTY;
        private final TextTemplate pluginTemplateShort = TextTemplate.EMPTY;
        private final TextTemplate pluginTemplateLong = TextTemplate.EMPTY;
        private final TextTemplate commandTemplateShort = TextTemplate.EMPTY;
        private final TextTemplate commandTemplateLong = TextTemplate.EMPTY;


        public final Topic fillRootTopic(final CommandManager manager, final Supplier<PermissionDescription.Builder> factory){
            final Set<PluginContainer> pluginConts = manager.getPluginContainers();
            final Comparator<PluginContainer> contAlpha = (a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getName(),b.getName());
            final Comparator<CommandMapping> mapAlpha = (a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getPrimaryAlias(),b.getPrimaryAlias());
            final Topic rootTopic = createRootTopic(factory);
            final Deque<Topic> allCommands = Lists.newLinkedList();
            pluginConts.stream().sorted(contAlpha).forEach(
                    cont -> {
                        final Topic pluginTopic = createPluginTopic(
                                cont.getName(),
                                rootTopic,
                                Text.of(cont.getDescription()),
                                Text.of(cont.getDescription()),
                                factory.get(),
                                "help" + '.' + cont.getUnqualifiedId()//Root node alias...
                        );
                        manager.getOwnedBy(cont).stream().sorted(mapAlpha).forEach(
                                mapping -> {
                                    final Topic command = createCommandTopic(cont, mapping, factory, pluginTopic);
                                    pluginTopic.subTopics.add(command);
                                }
                        );
                        rootTopic.subTopics.add(pluginTopic);
                    }
            );
            Sponge.getCommandManager().getCommands().stream().sorted(mapAlpha).forEach(
                    command-> {
                        final Topic t = createCommandTopic(pluginConts.stream().findFirst().get(),command,factory,rootTopic);
                        rootTopic.subTopics.add(t);
                    }
            );
            return rootTopic;
        }


        private Topic createRootTopic(final Supplier<PermissionDescription.Builder> factory){
            return new Topic(
                    "help",
                    null,
                    Text.of("."),
                    Text.of("click or hover on a topic, or click the arrows to navigate"),
                    factory.get(),
                    "help",
                    rootTemplateShort,
                    rootTemplateLong,
                    3
            );
        }

        private Topic createPluginTopic (
                final String key,
                final Topic parent,
                final Text shortDescription,
                final Text longDescription,
                final PermissionDescription.Builder pdBuilder,
                final String permission ) {
            return new Topic(key, parent, shortDescription, longDescription, pdBuilder, permission, pluginTemplateShort, pluginTemplateLong,
                    2);
        }

        private Topic createCommandTopic(
                final PluginContainer cont,
                final CommandMapping mapping,
                final Supplier<PermissionDescription.Builder> factory,
                final Topic parent
        ){
            final String primary = mapping.getPrimaryAlias();
            //Function<CommandSource, Text> aUsage = src -> mapping.getCallable().getShortDescription(src);
            final Set<String> allAliases = mapping.getAllAliases();
            //Function<CommandSource, Text> aShortDescription = src -> mapping.getCallable().getShortDescription(src);
            //Function<CommandSource, Text> aLongDescription = src -> mapping.getCallable().getHelp(src);
            return new Topic(
                primary,
                parent,
                Text.of(Topic.TopicConfigSerializer.SHORT_KEY),
                Text.of(Topic.TopicConfigSerializer.LONG_KEY),
                factory.get(),
                "help." + cont.getUnqualifiedId()+"."+primary,
                commandTemplateShort,
                commandTemplateLong,
                1
            );
        }

        @Override public final String toString() {
            return Objects.toStringHelper(this)
                    .add("rootTemplateShort", rootTemplateShort)
                    .add("rootTemplateLong", rootTemplateLong)
                    .add("pluginTemplateShort", pluginTemplateShort)
                    .add("pluginTemplateLong", pluginTemplateLong)
                    .add("commandTemplateShort", commandTemplateShort)
                    .add("commandTemplateLong", commandTemplateLong)
                    .toString();
        }
    }

    private static final class TopicConfigSerializer implements TypeSerializer<Topic>  {

        private static final String SHORT_KEY = "short";
        private static final String LONG_KEY = "long";
        private static final String KEY_KEY = "key";
        private static final String SUBTOPICS_KEY = "subtopics";
        private static final String PRIORITY_KEY = "order";
        private final PermissionDescription.Builder pdBuilder;

        private TopicConfigSerializer(final PermissionDescription.Builder pdBuilder) {
            this.pdBuilder = pdBuilder;
        }

        @Override public Topic deserialize(final TypeToken<?> type, final ConfigurationNode value) throws ObjectMappingException {
            return new Topic(  String.valueOf(value.getKey()),
                        null, //TODO:
                        value.getNode(SHORT_KEY).getValue(TypeToken.of(Text.class)),
                        value.getNode(LONG_KEY).getValue(TypeToken.of(Text.class)),
                        pdBuilder,
                        "", //permission
                        TextTemplate.EMPTY, //sTemplate
                        TextTemplate.EMPTY, //lTemplate
                        value.getNode(PRIORITY_KEY).getInt(7)
            );
        }

        @Override public void serialize(final TypeToken<?> type, final Topic obj, final ConfigurationNode value) throws ObjectMappingException {
            value.setValue(obj.toNodeInternal(value));
        }

        @Override public String toString() {
            return Objects.toStringHelper(this)
                    .add("pdBuilder", pdBuilder)
                    .toString();
        }
    }
}