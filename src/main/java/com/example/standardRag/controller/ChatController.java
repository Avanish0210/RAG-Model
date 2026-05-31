package com.example.standardRag.controller;

import com.example.standardRag.dto.ChatRequestDto;
import com.example.standardRag.dto.ChatResponseDto;
import com.example.standardRag.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/Documents")
public class ChatController {
    private final ChatService chatService;


    @RequestMapping(value = "/chat", method = {RequestMethod.GET, RequestMethod.POST})
    public ChatResponseDto chat(@RequestBody ChatRequestDto chatRequestDto) {
        return chatService.ask(chatRequestDto);
    }

}
