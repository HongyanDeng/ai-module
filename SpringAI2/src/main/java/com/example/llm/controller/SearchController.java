package com.example.llm.controller;

import com.example.llm.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchController {
    //查询材料控制层

    private final SearchService searchService;

    @RequestMapping("/searchResource")
    public String search(@RequestBody Map<String, Object> request) {
        /**
         * 从前端获取到查询条件
         */
        String personId = (String) request.get("personId");
        String name = (String) request.get("name");
        String type = (String) request.get("type");
        String searchContent = (String) request.get("searchContent");



        //searchService.searchResource(personId,name, type, searchContent);

        return searchService.searchResource(personId,name, type, searchContent);
    }
}
