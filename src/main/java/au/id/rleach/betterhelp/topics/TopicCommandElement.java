package au.id.rleach.betterhelp.topics;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.ArgumentParseException;
import org.spongepowered.api.command.args.CommandArgs;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.CommandElement;
import org.spongepowered.api.text.Text;

import java.util.List;

import javax.annotation.Nullable;

class TopicCommandElement extends CommandElement {

    private TopicCommandElement(@Nullable Text key) {
        super(key);
    }

    @Nullable @Override protected Object parseValue(CommandSource source, CommandArgs args) throws ArgumentParseException {
        return null;
    }

    @Override public List<String> complete(CommandSource src, CommandArgs args, CommandContext context) {
        return null;
    }
}
