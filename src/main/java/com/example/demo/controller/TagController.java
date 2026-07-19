package com.example.demo.controller;

import com.example.demo.dto.TagDto;
import com.example.demo.service.TagService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only REST API for the shared tag vocabulary. Tags are created implicitly
 * when content references them, so no write endpoints are exposed here.
 */
@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public List<TagDto> list() {
        return tagService.findAll().stream().map(TagDto::from).toList();
    }
}
