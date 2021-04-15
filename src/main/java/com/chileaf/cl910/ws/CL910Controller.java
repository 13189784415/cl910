package com.chileaf.cl910.ws;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CL910Controller {

    @GetMapping("/index")
    public ResponseEntity<String> index() {
        return ResponseEntity.ok("请求成功");
    }

}
