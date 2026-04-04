package com.callibrity.cowork.connector.prompts;

import com.callibrity.mocapi.prompts.GetPromptResult;
import com.callibrity.mocapi.prompts.PromptMessage;
import com.callibrity.mocapi.prompts.Role;
import com.callibrity.mocapi.prompts.annotation.Prompt;
import com.callibrity.mocapi.prompts.annotation.PromptService;
import com.callibrity.mocapi.prompts.content.TextContent;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@PromptService
public class SummaryPrompts {

    @Prompt(name = "summarize-text", description = "Summarize the provided text at a specified level of detail")
    public GetPromptResult summarizeText(
            @Schema(description = "The text content to summarize") String text,
            @Schema(description = "Desired summary length: 'brief' (1-2 sentences), 'standard' (1 paragraph), or 'detailed' (several paragraphs). Defaults to 'standard'.") String detail) {
        String detailLevel = detail == null || detail.isBlank() ? "standard" : detail.strip().toLowerCase();
        String instruction = switch (detailLevel) {
            case "brief" -> "Summarize the following text in 1-2 sentences, capturing only the most essential point.";
            case "detailed" -> "Provide a detailed summary of the following text, covering all main points, key arguments, and important details across several paragraphs.";
            default -> "Summarize the following text in a single paragraph, covering the main points clearly and concisely.";
        };

        String prompt = String.format("""
                %s

                ---

                %s
                """, instruction, text);

        return new GetPromptResult("Summarize provided text (" + detailLevel + ")", List.of(
                new PromptMessage(Role.USER, new TextContent(prompt))
        ));
    }

    @Prompt(name = "extract-action-items", description = "Extract a list of action items or tasks from the provided text")
    public GetPromptResult extractActionItems(
            @Schema(description = "The text to extract action items from (e.g. meeting notes, an email, a document)") String text) {
        String prompt = String.format("""
                Review the following text and extract all action items, tasks, or next steps mentioned.
                Format the result as a numbered list. For each item, include:
                - The action to be taken
                - The owner (if mentioned)
                - The deadline or due date (if mentioned)

                If no action items are found, say so clearly.

                ---

                %s
                """, text);

        return new GetPromptResult("Extract action items from text", List.of(
                new PromptMessage(Role.USER, new TextContent(prompt))
        ));
    }
}
