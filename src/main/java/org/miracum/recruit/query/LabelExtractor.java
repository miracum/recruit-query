package org.miracum.recruit.query;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class LabelExtractor {
    public Set<String> extract(String stringWithLabels) {
        if (StringUtils.isBlank(stringWithLabels)) {
            return Set.of();
        }

        var substrings = StringUtils.substringsBetween(stringWithLabels, "[", "]");

        if (substrings == null) {
            return Set.of();
        }

        return Sets.newHashSet(substrings);
    }
}
