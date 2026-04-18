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
      description = "${tools.feedback.submit-feedback.description}")
  public FeedbackAckDto submitFeedback(
      @Schema(description = "${tools.feedback.submit-feedback.tool-name.description}")
          String toolName,
      @Schema(description = "${tools.feedback.submit-feedback.type.description}") FrictionType type,
      @Schema(description = "${tools.feedback.submit-feedback.description-param.description}")
          String description,
      @Schema(description = "${tools.feedback.submit-feedback.suggested-change.description}")
          String suggestedChange) {
    if (log.isInfoEnabled()) {
      FeedbackPayload payload = new FeedbackPayload(toolName, type, description, suggestedChange);
      log.info("{}{}", MARKER, objectMapper.writeValueAsString(payload));
    }
    return new FeedbackAckDto(true);
  }

  private record FeedbackPayload(
      String toolName, FrictionType type, String description, String suggestedChange) {}
}
