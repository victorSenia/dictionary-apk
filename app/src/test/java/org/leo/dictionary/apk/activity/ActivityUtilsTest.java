package org.leo.dictionary.apk.activity;

import org.junit.Test;
import org.leo.dictionary.entity.Topic;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ActivityUtilsTest {

    @Test
    public void getTopicIds_returnsEmptySetForNullInput() {
        assertTrue(ActivityUtils.getTopicIds(null).isEmpty());
    }

    @Test
    public void getTopicIds_returnsUniqueTopicIds() {
        Topic first = new Topic();
        first.setId(10L);
        Topic duplicate = new Topic();
        duplicate.setId(10L);
        Topic second = new Topic();
        second.setId(20L);

        Set<Long> ids = ActivityUtils.getTopicIds(Arrays.asList(first, duplicate, second));

        assertEquals(2, ids.size());
        assertEquals(new HashSet<>(Arrays.asList(10L, 20L)), ids);
    }

    @Test
    public void getTopicIds_returnsEmptySetForEmptyCollection() {
        assertTrue(ActivityUtils.getTopicIds(Collections.emptyList()).isEmpty());
    }
}
