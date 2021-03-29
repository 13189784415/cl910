package com.chileaf.cl900.ws;

import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@RestController
public class CL900Controller {

    @GetMapping("/index")
    public ResponseEntity<String> index() {
        return ResponseEntity.ok("请求成功");
    }

}
