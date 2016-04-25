package au.id.rleach.betterhelp.topics;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

import java.util.Comparator;

public class TopicStore {
    //parent, child.
    private final Multimap<Topic, Topic> mmap;
    private static final Comparator<Topic> COMPARATOR = TopicPriorityComparator.PRIORITY;
    public TopicStore() {
        mmap = TreeMultimap.create(COMPARATOR, COMPARATOR);
    }
}
