package com.guyi.kindredspirits.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 聊天接口
 *
 * @author 孤诣
 */
@RestController
@RequestMapping("/chat")
@Slf4j
@CrossOrigin(origins = {"http://127.0.0.1:3000", "http://localhost:3000"}, allowCredentials = "true")
public class ChatController {

}
