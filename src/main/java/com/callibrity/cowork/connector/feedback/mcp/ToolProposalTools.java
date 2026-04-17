/*
 * Copyright © 2026 Callibrity, Inc. (contactus@callibrity.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.callibrity.cowork.connector.feedback.mcp;

import com.callibrity.cowork.connector.feedback.dto.FeedbackAckDto;
import com.callibrity.cowork.connector.feedback.dto.Frequency;
import com.callibrity.mocapi.api.tools.ToolMethod;
import com.callibrity.mocapi.api.tools.ToolService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * One MCP tool that lets the calling LLM propose a brand-new tool the server should add. Companion
 * to {@link FeedbackTools} — friction with existing tools goes there, missing-tool proposals go
 * here. Each call emits an INFO log line prefixed with {@code MCP_TOOL_PROPOSAL:} followed by a
 * JSON payload, intended to be aggregated by a downstream agent that drafts PRs adding the
 * highest-conviction proposals.
 */
@ToolService
@Component
@RequiredArgsConstructor
public class ToolProposalTools {

  private static final Logger log = LoggerFactory.getLogger(ToolProposalTools.class);
  private static final String MARKER = "MCP_TOOL_PROPOSAL: ";

  private final ObjectMapper objectMapper;

  @ToolMethod(
      name = "suggest-tool",
      title = "Suggest a New Tool",
      description =
          """
                    Propose a brand-new tool this MCP server should add. Use this when, after
                    completing a task, you noticed: "I kept needing X but no single tool gave me
                    X cleanly."

                    USE THIS WHEN:
                      - You finished a multi-step task and a single hypothetical tool would have
                        replaced several actual calls.
                      - You hit a question this session that no existing tool answers, and you
                        worked around the absence by composing other tools awkwardly.
                      - The proposed tool would address a recurring pattern of question, not a
                        one-off curiosity.

                    DO NOT USE THIS FOR:
                      - Small additions to an existing tool (a missing field, a new filter, a
                        renamed parameter). Use submit-feedback instead — that's a friction
                        report on an existing tool, not a new tool.
                      - Tools you didn't actually need this session. "Wouldn't it be nice if..."
                        proposals are noise; ground every proposal in a question you actually
                        had to answer.
                      - Proposals where you can't articulate what existing tools you tried first.
                        If the existingToolGap field would be vague, the proposal isn't
                        grounded enough — don't submit.

                    Submit one call per distinct proposal. If you didn't need a new tool this
                    session, do not call this — silence is the most useful signal.""")
  public FeedbackAckDto proposeTool(
      @Schema(
              description =
                  "Short kebab-case name you'd give the tool, e.g. 'service-runbook-history'.")
          String proposedName,
      @Schema(
              description =
                  "One sentence: what the tool would return and which question it would answer.")
          String purpose,
      @Schema(
              description =
                  "Free-form description of inputs the tool would take. Name the params and types, e.g. 'name: string (required), since: ISO date (optional)'.")
          String inputs,
      @Schema(
              description =
                  "Free-form description of the output shape, e.g. 'list of {revision, author, date, summary} ordered newest-first'.")
          String output,
      @Schema(
              description =
                  "The actual user question from this session that made you want this tool. Quote it if possible.")
          String motivatingQuestion,
      @Schema(
              description =
                  "Which existing tools you tried for this question and why they didn't suffice. If you can't be specific here, don't submit.")
          String existingToolGap,
      @Schema(
              description =
                  "How often this tool would be needed: ONCE_THIS_SESSION (truly one-off), RECURRING_PATTERN (you'd reach for it across many sessions), or FOUNDATIONAL (would unblock entire categories of questions).")
          Frequency frequency) {
    if (log.isInfoEnabled()) {
      ProposalPayload payload =
          new ProposalPayload(
              proposedName,
              purpose,
              inputs,
              output,
              motivatingQuestion,
              existingToolGap,
              frequency);
      log.info("{}{}", MARKER, objectMapper.writeValueAsString(payload));
    }
    return new FeedbackAckDto(true);
  }

  private record ProposalPayload(
      String proposedName,
      String purpose,
      String inputs,
      String output,
      String motivatingQuestion,
      String existingToolGap,
      Frequency frequency) {}
}
