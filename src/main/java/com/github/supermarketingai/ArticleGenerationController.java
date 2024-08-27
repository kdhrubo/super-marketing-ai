package com.github.supermarketingai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.micrometer.common.util.StringUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/api/articles")
@RequiredArgsConstructor
public class ArticleGenerationController {

    @Value("${OPENAI_API_KEY}")
    private String OPENAI_API_KEY;

    @Value("classpath:/prompt/article-ideas.st")
    private Resource articleIdeasPromptResource;

    @Value("classpath:/prompt/schedule-articles.st")
    private Resource articleScheduleResource;


    private final ObjectMapper objectMapper;

    @PostMapping("/schedule")
    public Object createSchedule(@RequestBody @Valid AcceptedArticles acceptedArticles) {
        log.info("Accepted articles: {}", acceptedArticles);

        ChatLanguageModel chatLanguageModel = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName("gpt-4o-mini")
                .responseFormat("json_object")
                .build();

        try {
            String promptStr = StreamUtils.copyToString(articleScheduleResource.getInputStream(), Charset.defaultCharset());

            PromptTemplate promptTemplate = PromptTemplate.from(promptStr);
            Prompt prompt = promptTemplate.apply(Map.of(
                            "accepted_articles_idea", acceptedArticles.acceptedArticles(),
                            "no_of_articles",2

                    )

            );


            long start = System.currentTimeMillis();


            String json =
                    chatLanguageModel
                            .generate(prompt.toUserMessage()).content().text();

            log.info("json - {}",json);


            Map<String, Object> result = objectMapper.readValue(json, new TypeReference<>() {});

            log.info("Time - {} ms", System.currentTimeMillis() - start);

            log.info("Result: {}", result);

            return result.get("articles");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @PostMapping("/ideate")
    public List<String> getContentIdeas(@RequestBody @Valid ArticleIdea articleIdea) {
        ChatLanguageModel chatLanguageModel = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName("gpt-4o-mini")
                .responseFormat("json_object")
                .build();

        try {
            String promptStr = StreamUtils.copyToString(articleIdeasPromptResource.getInputStream(), Charset.defaultCharset());

            PromptTemplate promptTemplate = PromptTemplate.from(promptStr);
            Prompt prompt = promptTemplate.apply(Map.of(
                    "subject_details", articleIdea.subjectDetail(),
                    "goals_and_intention", articleIdea.goalIntention(),
                    "count_of_ideas", articleIdea.countOfIdeas()

                    )

            );


            long start = System.currentTimeMillis();

            String json =
            chatLanguageModel
                    .generate(prompt.toUserMessage()).content().text();


            Map<String, Object> result = objectMapper.readValue(json, new TypeReference<>() {});

            log.info("Time - {} ms", System.currentTimeMillis() - start);

            log.info("Result: {}", result);

            return (List<String>) result.get("contentIdeas");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
