package org.miracum.recruit.query;

import com.google.common.collect.Sets;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class LabelExtractor {

  public Set<String> extract(String stringWithLabels) {
    if (StringUtils.isBlank(stringWithLabels)) {
      return Set.of();
    }
    return extractAll(stringWithLabels);
  }

  /**
   * Checks if Set of Labels contains a specific tag with format [tag=xxx]
   *
   * @param labels set of all labels
   * @return extracted acronym
   */
  public String extractByTag(String tag, String stringWithLabels) {
    Set<String> labels = extractAll(stringWithLabels);
    String value = "";
    for (String label : labels) {
      if (label.contains(tag)) {
        value = label.split("=")[1];
      }
    }
    if (value.isBlank()) {
      return null;
    }
    value = value.trim();
    return value;
  }

  public Set<String> extractAll(String stringWithLabels) {
    var substrings = StringUtils.substringsBetween(stringWithLabels, "[", "]");
    if (substrings == null) {
      return Set.of();
    }
    return Sets.newHashSet(substrings);
  }
}
