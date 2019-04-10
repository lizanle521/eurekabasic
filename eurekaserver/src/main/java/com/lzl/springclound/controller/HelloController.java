package com.lzl.springclound.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author lizanle
 * @data 2019/4/10 9:21 PM
 */
@RestController
public class HelloController {

    @GetMapping("/hello")
    public String get(){
        return "hello";
    }
}
