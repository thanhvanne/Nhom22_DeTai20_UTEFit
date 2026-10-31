package com.utefit.dto.chat;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessageSendRequest {

    @com.utefit.validation.ValidInput("id")
    @jakarta.validation.constraints.NotNull(message = "Vui lòng chọn hội thoại.")
    private Long conversationId;
    @com.utefit.validation.ValidInput("content")
    @jakarta.validation.constraints.NotBlank(message = "Nội dung tin nhắn không được để trống.")
    private String content;
}
