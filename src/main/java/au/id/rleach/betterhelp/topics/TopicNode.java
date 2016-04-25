package au.id.rleach.betterhelp.topics;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Represents a key to a topic, represented as a "permmision like" node that specifys it's parents.
 * escapes strings passed to it, assumes arrays passed are already unescaped.
 *
 * Escapes "." as "\." and " " as "\ "
 */
class TopicNode implements TypeSerializer<TopicNode> {

    private static final char TOPIC_SEPARATOR = '.';
    private final String[] path;

    TopicNode(final String[] path) {
        this.path = Arrays.copyOf(path, path.length);
    }

    TopicNode(final String path) {
        this.path = CommandEscaper.unescape(path).split("\\.");
    }

    private String stringPath(){
        final Stream<String> pathStream = Arrays.stream(path).map(CommandEscaper::escape);
        return Joiner.on(TOPIC_SEPARATOR).join(pathStream.iterator());
    }

    private int getDepth(){
        return path.length - 1;
    }

    @Override public final String toString() {
        return Objects.toStringHelper(this)
                .add( "path" , stringPath() )
                .add( "depth", getDepth()   )
                .toString();
    }

    @Override public final TopicNode deserialize(final TypeToken<?> type, final ConfigurationNode value) throws ObjectMappingException {
        return new TopicNode(value.getString());
    }

    @Override public final void serialize(final TypeToken<?> type, final TopicNode obj, final ConfigurationNode value) throws ObjectMappingException {
        value.setValue(stringPath());
    }
}
