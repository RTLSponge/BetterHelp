package au.id.rleach.betterhelp.topics;

import org.apache.commons.lang3.text.translate.AggregateTranslator;
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;
import org.apache.commons.lang3.text.translate.LookupTranslator;

final class CommandEscaper {
    private static final CharSequenceTranslator ESCAPE_TOPIC = new AggregateTranslator(new LookupTranslator(
            new String[][] {
                    {".", "\\."},
                    {" ", "\\ "},
                    {"\\", "\\\\"}
            })
    );

    private static final CharSequenceTranslator UNESCAPE_TOPIC =
            new AggregateTranslator(
                    new LookupTranslator(
                            new String[][] {
                                    {"\\\\", "\\"},
                                    {"\\.", "."},
                                    {"\\ ", " "},
                                    {"\\", ""}
                            })
            );

    public static String escape(String s){
        return ESCAPE_TOPIC.translate(s);
    }

    public static String unescape(String s){
        return UNESCAPE_TOPIC.translate(s);
    }

    private CommandEscaper() {
    }
}
