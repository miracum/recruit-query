package org.miracum.recruit.query;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
public class LabelExtractorTests {

    private final LabelExtractor sut = new LabelExtractor();

    @Test
    public void extract_WithEmptyString_returnsEmptySet() {
        var result = sut.extract("");

        assertThat(result).isEmpty();
    }

    @Test
    public void extract_WithNullString_returnsDisctinct() {
        var result = sut.extract(null);

        assertThat(result).isEmpty();
    }

    @Test
    public void extract_WithDuplicateLabels_returnsDisctinct() {
        var result = sut.extract("[a] abc [b] [a]");

        assertThat(result).contains("a", "b");
    }

    @Test
    public void extract_WithoutLabels_returnsEmptySet() {
        var result = sut.extract("hello world ]");

        assertThat(result).isEmpty();
    }
}
