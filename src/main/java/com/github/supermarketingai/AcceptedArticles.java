package com.github.supermarketingai;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AcceptedArticles(
        @Positive(message = "No of articles must be greater than or equal to one.")
        int noOfArticlesPerWeek,
        @Size(min = 1, max = 25, message = "Minimum 1 and maximum 25 article ideas can be submitted")
        @NotNull(message = "The selected articles list cannot be empty")
        List<String> acceptedArticles) {
}
