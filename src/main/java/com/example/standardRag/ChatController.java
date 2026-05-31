package com.example.standardRag;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/Documents")
public class ChatController {
    private final ChatClient chatClient;


    @GetMapping("/chat")
    public String chat(@RequestParam String query) {
        return chatClient.prompt()
                .user(query)
                .call()
                .content();
    }
}
