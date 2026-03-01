package com.waitlist.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping(value = {
        "/{path:(?!api|assets|css|js|images|fonts|static)[^\\.]*}",
        "/{path1:(?!api)[^\\.]*}/{path2:[^\\.]*}",
        "/{path1:(?!api)[^\\.]*}/{path2:[^\\.]*}/{path3:[^\\.]*}",
        "/{path1:(?!api)[^\\.]*}/{path2:[^\\.]*}/{path3:[^\\.]*}/{path4:[^\\.]*}"
    })
    public String redirect() {
        return "forward:/index.html";
    }
}
