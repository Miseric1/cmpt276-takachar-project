package com.example.demo.service.sentiment;

import com.example.demo.model.SentimentLabel;

public record SentimentResult(SentimentLabel label, Double confidence, String model) {
}
