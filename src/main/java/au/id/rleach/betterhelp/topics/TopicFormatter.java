package au.id.rleach.betterhelp.topics;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.text.TextTemplate;


/**
 * A formatter that reads from a config and formats topics. TODO: Should this be split into a TopicTemplate and a TopicFormatter?
 */
@ConfigSerializable
public class TopicFormatter {
    @Setting
    TextTemplate shortTemplate;
    TextTemplate longTemplate;



}
