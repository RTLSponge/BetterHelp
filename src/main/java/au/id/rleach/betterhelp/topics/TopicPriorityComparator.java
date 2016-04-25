package au.id.rleach.betterhelp.topics;

import com.google.common.collect.ComparisonChain;

import java.io.Serializable;
import java.util.Comparator;

public class TopicPriorityComparator {

    public static final Comparator<Topic> PRIORITY = new TopicPriorityComparator.PriorityComparator();
    public static Comparator<Topic> DEPTH = new TopicPriorityComparator.TopicDepthComparator();

    private static class PriorityComparator implements Comparator<Topic>, Serializable {
        static final int ROOT = 6;
        static final int PLUGIN = 4;
        static final int COMMAND = 2;
        private static final long serialVersionUID = 4733567654688956071L;

        @Override public final int compare(final Topic left, final Topic right) {
            return ComparisonChain.start().compare(left.priority, right.priority).compare(left.key, right.key).result();
        }
    }

    private static class TopicDepthComparator implements Comparator<Topic>, Serializable {

        private static final long serialVersionUID = -1257457451420661459L;

        @Override public int compare(Topic o1, Topic o2) {
            return ComparisonChain.start().compare(o1.depth, o2.depth).result();
        }
    }
}
