package au.id.rleach.betterhelp;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.plugin.Plugin;

import java.util.Optional;

@Plugin(id = "betterhelp", name = "BetterHelp")
public class BetterHelp {

    @Listener
    public void onStart(GamePreInitializationEvent event){
        Optional<? extends CommandMapping> helpMapping = Sponge.getCommandManager().get("help");
        helpMapping.ifPresent(mapping->
                Sponge.getCommandManager().removeMapping(mapping)
        );
        Sponge.getCommandManager().register(this, SpongeHelpCommand.create(), "help");
    }
}


