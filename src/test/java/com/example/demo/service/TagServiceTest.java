package com.example.demo.service;

import com.example.demo.model.Tag;
import com.example.demo.repository.TagRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    private TagService tagService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tagService = new TagService(tagRepository);
    }

    @Test
    void findAllDelegatesToRepository() {
        Tag tag = new Tag("network");
        when(tagRepository.findAll()).thenReturn(List.of(tag));

        assertThat(tagService.findAll()).containsExactly(tag);
    }

    @Test
    void resolveTagsReturnsEmptySetWhenNamesNull() {
        assertThat(tagService.resolveTags(null)).isEmpty();
        verifyNoInteractions(tagRepository);
    }

    @Test
    void resolveTagsIgnoresBlankAndNullEntries() {
        Set<String> raw = new LinkedHashSet<>();
        raw.add(null);
        raw.add("");
        raw.add("   ");

        assertThat(tagService.resolveTags(raw)).isEmpty();
        verifyNoInteractions(tagRepository);
    }

    @Test
    void resolveTagsNormalizesToLowerCaseAndTrims() {
        when(tagRepository.findByNameIgnoreCase("printer")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        Set<Tag> tags = tagService.resolveTags(Set.of("  Printer  "));

        assertThat(tags).hasSize(1);
        assertThat(tags.iterator().next().getName()).isEqualTo("printer");
    }

    @Test
    void resolveTagsReusesExistingTagInsteadOfCreating() {
        Tag existing = new Tag("network");
        when(tagRepository.findByNameIgnoreCase("network")).thenReturn(Optional.of(existing));

        Set<Tag> tags = tagService.resolveTags(Set.of("network"));

        assertThat(tags).containsExactly(existing);
        verify(tagRepository, never()).save(any());
    }

    @Test
    void resolveTagsCreatesMissingTags() {
        when(tagRepository.findByNameIgnoreCase("billing")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        Set<Tag> tags = tagService.resolveTags(Set.of("billing"));

        assertThat(tags).extracting(Tag::getName).containsExactly("billing");
        verify(tagRepository).save(any(Tag.class));
    }

    @Test
    void resolveTagsDeduplicatesCaseInsensitiveDuplicates() {
        Tag existing = new Tag("printer");
        when(tagRepository.findByNameIgnoreCase("printer")).thenReturn(Optional.of(existing));

        Set<String> raw = new LinkedHashSet<>();
        raw.add("Printer");
        raw.add("printer");
        raw.add("  PRINTER  ");

        Set<Tag> tags = tagService.resolveTags(raw);

        assertThat(tags).containsExactly(existing);
        verify(tagRepository, times(3)).findByNameIgnoreCase("printer");
    }
}
