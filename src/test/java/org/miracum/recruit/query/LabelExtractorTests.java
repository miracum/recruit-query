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

    @Test
    public void extractTag_oneTag_returnsOneTag() {
    	var result = sut.extractByTag("acronym", "[acronym=Test]");
    	assertThat(result).isEqualTo("Test");
    }

    @Test
    public void extractTag_twoTags_returnsRightTag() {
    	var result = sut.extractByTag("acronym", "[acronym=Test] [hello=World]");
    	assertThat(result).isEqualTo("Test");
    }

    @Test
    public void extractTag_longTextWithOneTag_returnsOneTag() {
    	var result = sut.extractByTag("acronym", "This is a description with one acronym [acronym=Test] and label [Testlabel]");
    	assertThat(result).isEqualTo("Test");
    }

    @Test
    public void extractTag_longTextWithoutTag_returnsEmpty() {
    	var result = sut.extractByTag("acronym", "This is a description without a tag but with [label]");
    	assertThat(result).isBlank();
    }
}
