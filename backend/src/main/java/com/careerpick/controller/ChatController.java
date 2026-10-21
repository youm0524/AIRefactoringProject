package com.careerpick.controller;

import com.careerpick.dto.ChatMessage;
import com.careerpick.dto.SendMessageRequest;
import com.careerpick.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/jobs/{jobId}/messages")
@CrossOrigin(origins = "http://localhost:5173")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) { this.chatService = chatService; }

    @GetMapping
    public List<ChatMessage> messages(@PathVariable long jobId) {
        return chatService.messages(jobId);
    }

    @PostMapping
    public List<ChatMessage> send(@PathVariable long jobId, @RequestHeader(name="Authorization", required=false) String authorization,
                                  @Valid @RequestBody SendMessageRequest request) {
        return chatService.send(jobId, authorization, request.text());
    }
}
