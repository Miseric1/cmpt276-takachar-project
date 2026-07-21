package com.example.demo.repository;

import com.example.demo.model.Tag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TagRepositoryTest {

    @Autowired
    private TagRepository tagRepository;

    @Test
    void findByNameIgnoreCaseMatchesRegardlessOfCase() {
        tagRepository.save(new Tag("printer"));

        Optional<Tag> found = tagRepository.findByNameIgnoreCase("PRINTER");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("printer");
    }

    @Test
    void findByNameIgnoreCaseReturnsEmptyWhenAbsent() {
        assertThat(tagRepository.findByNameIgnoreCase("missing")).isEmpty();
    }
}
