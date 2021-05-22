package org.miracum.recruit.query;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LabelExtractorTests {

  private final LabelExtractor sut = new LabelExtractor();

  @Test
  void extract_WithDuplicateLabels_returnsDisctinct() {
    var result = sut.extract("[a] abc [b] [a]");

    assertThat(result).contains("a", "b");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "hello world ]"})
  void extract_withGivenString_returnsEmptySet() {
    var result = sut.extract("");

    assertThat(result).isEmpty();
  }

  @Test
  void extract_withNullString_returnsEmptySet() {
    var result = sut.extract(null);

    assertThat(result).isEmpty();
  }

  @Test
  void extractTag_longTextWithOneTag_returnsOneTag() {
    var result =
        sut.extractByTag(
            "acronym",
            "This is a description with one acronym [acronym=Test] and label [Testlabel]");
    assertThat(result).isEqualTo("Test");
  }

  @Test
  void extractTag_longTextWithoutTag_returnsEmpty() {
    var result =
        sut.extractByTag("acronym", "This is a description without a tag but with [label]");
    assertThat(result).isBlank();
  }

  @Test
  void extractTag_oneTag_returnsOneTag() {
    var result = sut.extractByTag("acronym", "[acronym=Test]");
    assertThat(result).isEqualTo("Test");
  }

  @Test
  void extractTag_twoTags_returnsRightTag() {
    var result = sut.extractByTag("acronym", "[acronym=Test] [hello=World]");
    assertThat(result).isEqualTo("Test");
  }
}
