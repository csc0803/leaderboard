package com.chun.backend.exception;

public class PostNotFoundException extends RuntimeException {

    public PostNotFoundException(Long id){
        super("Post not found: " + id);
    }
}
