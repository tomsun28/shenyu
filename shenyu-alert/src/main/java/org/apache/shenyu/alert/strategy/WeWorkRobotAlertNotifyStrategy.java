/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shenyu.alert.strategy;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.shenyu.alert.exception.AlertNoticeException;
import org.apache.shenyu.alert.model.AlertReceiverDTO;
import org.apache.shenyu.common.dto.AlarmContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

/**
 * Send alarm information through enterprise WeChat
 */
@Service
final class WeWorkRobotAlertNotifyStrategy extends AbstractAlertNotifyHandler {

    private static final Logger log = LoggerFactory.getLogger(WeWorkRobotAlertNotifyStrategy.class);

    public static final String WEBHOOK_URL = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=";
    
    @Override
    public void send(final AlertReceiverDTO receiver, final AlarmContent alert) {
        try {
            WeWorkWebHookDto weWorkWebHookDTO = new WeWorkWebHookDto();
            WeWorkWebHookDto.MarkdownDTO markdownDTO = new WeWorkWebHookDto.MarkdownDTO();
            markdownDTO.setContent(renderContent(alert));
            weWorkWebHookDTO.setMarkdown(markdownDTO);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<WeWorkWebHookDto> httpEntity = new HttpEntity<>(weWorkWebHookDTO, headers);
            String webHookUrl = WEBHOOK_URL + receiver.getWechatId();
            ResponseEntity<CommonRobotNotifyResp> entity = getRestTemplate().postForEntity(webHookUrl, httpEntity, CommonRobotNotifyResp.class);
            if (entity.getStatusCode() == HttpStatus.OK) {
                assert entity.getBody() != null;
                if (entity.getBody().getErrCode() == 0) {
                    log.debug("Send WeWork webHook: {} Success", webHookUrl);
                } else {
                    log.warn("Send WeWork webHook: {} Failed: {}", webHookUrl, entity.getBody().getErrMsg());
                    throw new AlertNoticeException(entity.getBody().getErrMsg());
                }
            } else {
                log.warn("Send WeWork webHook: {} Failed: {}", webHookUrl, entity.getBody());
                throw new AlertNoticeException("Http StatusCode " + entity.getStatusCode());
            }
        } catch (Exception e) {
            throw new AlertNoticeException("[WeWork Notify Error] " + e.getMessage());
        }
    }

    @Override
    public byte type() {
        return 4;
    }

    @Override
    protected String templateName() {
        return "alertNotifyWeWorkRobot";
    }

    /**
     * WeWork Body.
     */
    private static class WeWorkWebHookDto {
        
        /**
         * markdown type.
         */
        private static final String MARKDOWN = "markdown";
        /**
         * test type.
         */
        private static final String TEXT = "text";

        /**
         * message type.
         */
        @JsonProperty(value = "msgtype")
        private String msgType = MARKDOWN;

        /**
         * markdown message.
         */
        private MarkdownDTO markdown;

        /**
         * get message type.
         * @return type
         */
        public String getMsgType() {
            return msgType;
        }

        /**
         * set message type.
         * @param msgType type
         */
        public void setMsgType(String msgType) {
            this.msgType = msgType;
        }

        /**
         * get markdown.
         * @return markdown
         */
        public MarkdownDTO getMarkdown() {
            return markdown;
        }

        /**
         * set markdown.
         * @param markdown markdown
         */
        public void setMarkdown(MarkdownDTO markdown) {
            this.markdown = markdown;
        }

        /**
         * Markdown Body.
         */
        public static class MarkdownDTO {
            /**
             * message content.
             */
            private String content;

            /**
             * get content.
             * @return content
             */
            public String getContent() {
                return content;
            }

            /**
             * set content.
             * @param content content
             */
            public void setContent(String content) {
                this.content = content;
            }
        }

    }
}
