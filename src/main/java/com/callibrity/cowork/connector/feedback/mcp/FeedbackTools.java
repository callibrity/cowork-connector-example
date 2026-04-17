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
import com.callibrity.cowork.connector.feedback.dto.FrictionType;
import com.callibrity.mocapi.api.tools.ToolMethod;
import com.callibrity.mocapi.api.tools.ToolService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * One MCP tool that lets the calling LLM submit a single piece of friction-feedback about this
 * server's tool surface. The whole feature is the log line: each call emits an INFO event prefixed
 * with {@code MCP_FEEDBACK:} followed by a JSON payload, ready to be scraped from Log Analytics and
 * fed into a downstream aggregator.
 */
@ToolService
@Component
@RequiredArgsConstructor
public class FeedbackTools {

  private static final Logger log = LoggerFactory.getLogger(FeedbackTools.class);
  private static final String MARKER = "MCP_FEEDBACK: ";

  private final ObjectMapper objectMapper;

  @ToolMethod(
      name = "submit-feedback",
      title = "Submit Tool-Friction Feedback",
      description =
          """
                    Submit ONE piece of feedback about a specific friction you experienced using
                    this MCP server's tools during this conversation.

                    USE THIS WHEN you hit a concrete friction such as:
                      - A tool's response was missing a field you needed, forcing a follow-up call.
                      - You had to make multiple calls to assemble information that should have
                        been one call.
                      - A tool's input shape didn't naturally match how the user asked the
                        question.
                      - A field or tool name was ambiguous and you had to guess its meaning.
                      - A tool returned data in a shape that was awkward to reason about.

                    DO NOT USE THIS FOR:
                      - General praise ("the tools were great").
                      - Brand-new tool ideas — use suggest-tool instead. This tool is for
                        friction with tools that already exist.
                      - Feedback you didn't actually experience — fabricated feedback is worse
                        than silence.

                    If you didn't hit any friction, do not call this tool at all — that silence
                    is itself the most useful signal. Submit one call per distinct friction.
                    Maintainers review aggregated feedback to evolve this server's design.""")
  public FeedbackAckDto submitFeedback(
      @Schema(
              description =
                  "Short name of the tool that caused the friction (e.g. 'services-list'). Use 'none' if the friction is the absence of a tool.")
          String toolName,
      @Schema(
              description =
                  "Category of friction. One of: MISSING_FIELD, EXTRA_ROUND_TRIPS, AWKWARD_SCHEMA, AMBIGUOUS_NAMING, AWKWARD_RESPONSE_SHAPE.")
          FrictionType type,
      @Schema(
              description =
                  "What you tried, what happened, and why it was awkward. 1-3 sentences. Be specific — name the fields, name the calls, quote the exact value if it matters.")
          String description,
      @Schema(
              description =
                  "Concrete suggested change. State a position: 'add field X to ServiceDto', 'split services-list into two tools', 'rename field Y to Z'. If you can't articulate a concrete change, the friction probably isn't real enough to submit.")
          String suggestedChange) {
    FeedbackPayload payload = new FeedbackPayload(toolName, type, description, suggestedChange);
    log.info("{}{}", MARKER, objectMapper.writeValueAsString(payload));
    return new FeedbackAckDto(true);
  }

  private record FeedbackPayload(
      String toolName, FrictionType type, String description, String suggestedChange) {}
}
