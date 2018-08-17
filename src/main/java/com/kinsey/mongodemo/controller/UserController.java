package com.kinsey.mongodemo.controller;

import com.kinsey.mongodemo.entity.User;
import com.kinsey.mongodemo.mapper.PersonMapper;
import com.kinsey.mongodemo.model.PageVO;
import com.kinsey.mongodemo.model.PersonFilter;
import com.kinsey.mongodemo.model.PersonModel;
import com.kinsey.mongodemo.model.PersonRequest;
import com.kinsey.mongodemo.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

/**
 * Created by zj on 2018/8/6
 */
@RestController
@RequestMapping("/person")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public void create(@RequestBody PersonRequest request) {
        User person = PersonMapper.getInstance().PersonRequestToPerson(request);
        userService.createPerson(person);
    }

    @GetMapping
    public PageVO<PersonModel> list(@RequestParam("page") int page,
                                    @RequestParam("size") int size,
                                    @RequestParam(value = "search", required = false) String search) {
        PersonFilter filter = PersonFilter.builder().search(search).build();
        return userService.list(PageRequest.of(page,size),filter);
    }

    @GetMapping("/{id}")
    public PersonModel findById(@PathVariable String id) {
        return userService.findById(id);
    }

    @DeleteMapping()
    public void deleteAll(){
        userService.deleteAll();
    }

}