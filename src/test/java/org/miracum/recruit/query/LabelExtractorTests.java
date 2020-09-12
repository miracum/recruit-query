package org.miracum.recruit.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LabelExtractorTests {

    private final LabelExtractor sut = new LabelExtractor();

    @Test
    void extract_WithEmptyString_returnsEmptySet() {
        var result = sut.extract("");

        assertThat(result).isEmpty();
    }

    @Test
    void extract_WithNullString_returnsDisctinct() {
        var result = sut.extract(null);

        assertThat(result).isEmpty();
    }

    @Test
    void extract_WithDuplicateLabels_returnsDisctinct() {
        var result = sut.extract("[a] abc [b] [a]");

        assertThat(result).contains("a", "b");
    }

    @Test
    void extract_WithoutLabels_returnsEmptySet() {
        var result = sut.extract("hello world ]");

        assertThat(result).isEmpty();
    }
}
