package com.github.supermarketingai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
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
public class ArticleWriterController {

    @Value("${OPENAI_API_KEY}")
    private String OPENAI_API_KEY;


    @Value("classpath:/prompt/write-articles-system.st")
    private Resource writeArticlesSystem;

    @Value("classpath:/prompt/write-articles-step1-gather-details.st")
    private Resource writeArticlesStep1GatherDetails;

    @Value("classpath:/prompt/write-articles-step2-generate-subsections.st")
    private Resource writeArticlesStep2GenerateSubsections;


    private final ObjectMapper objectMapper;

    ChatMemory chatMemory = TokenWindowChatMemory.withMaxTokens(400, new OpenAiTokenizer());


    @PostMapping("/create-subsections")
    public Object createSubsections(@RequestBody @Valid ArticleSubsectionRequest articleSubsectionRequest) {
        log.info("articleSubsectionRequest : {}", articleSubsectionRequest);

        ChatLanguageModel chatLanguageModel = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName("gpt-4o-mini")
                .responseFormat("json_object")
                .build();

        try {
            String promptStr = StreamUtils.copyToString(writeArticlesStep2GenerateSubsections.getInputStream(), Charset.defaultCharset());

            PromptTemplate promptTemplate = PromptTemplate.from(promptStr);
            Prompt prompt = promptTemplate.apply(Map.of(
                            "question_answer", articleSubsectionRequest.questionAnswerPairList()
                    )

            );


            chatMemory.add(prompt.toUserMessage());

            log.info("Chat memory id = {}", chatMemory.id());

            long start = System.currentTimeMillis();


            String json =
                    chatLanguageModel
                            .generate(chatMemory.messages()).content().text();

            log.info("json - {}",json);


            Map<String, Object> result = objectMapper.readValue(json, new TypeReference<>() {});

            log.info("Time - {} ms", System.currentTimeMillis() - start);

            log.info("Result: {}", result);

            chatMemory.add(UserMessage.from(json));

            log.info("Chat memory id = {}", chatMemory.id());

            return result.get("subtopics");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    @PostMapping("/gather-info")
    public Object gatherInfo(@RequestBody @Valid ArticleRequirement articleRequirement) {
        log.info("articleRequirement : {}", articleRequirement);

        ChatLanguageModel chatLanguageModel = OpenAiChatModel.builder()
                .apiKey(OPENAI_API_KEY)
                .modelName("gpt-4o-mini")
                .responseFormat("json_object")
                .build();

        try {

            String promptStrSys = StreamUtils.copyToString(writeArticlesSystem.getInputStream(), Charset.defaultCharset());

            PromptTemplate promptTemplateSys = PromptTemplate.from(promptStrSys);
            Prompt promptSys = promptTemplateSys.apply(Map.of(
                            "subject", articleRequirement.expertise(),
                            "article_title", articleRequirement.title()


                    )

            );

            chatMemory.add(promptSys.toSystemMessage());


            String promptStr = StreamUtils.copyToString(writeArticlesStep1GatherDetails.getInputStream(), Charset.defaultCharset());

            PromptTemplate promptTemplate = PromptTemplate.from(promptStr);
            Prompt prompt = promptTemplate.apply(Map.of(
                            "subject", articleRequirement.expertise(),
                            "article_title", articleRequirement.title()


                    )

            );


            chatMemory.add(prompt.toUserMessage());

            log.info("Chat memory id = {}", chatMemory.id());

            long start = System.currentTimeMillis();


            String json =
                    chatLanguageModel
                            .generate(chatMemory.messages()).content().text();

            log.info("json - {}",json);


            Map<String, Object> result = objectMapper.readValue(json, new TypeReference<>() {});

            log.info("Time - {} ms", System.currentTimeMillis() - start);

            log.info("Result: {}", result);

            chatMemory.add(UserMessage.from(json));

            log.info("Chat memory id = {}", chatMemory.id());

            return result.get("questions");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


}
