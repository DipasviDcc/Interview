package org.example.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> invalid(MethodArgumentNotValidException e) {
        return Map.of("error", e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage()).distinct().collect(Collectors.joining("; ")));
    }
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Map<String, String> malformed() { return Map.of("error", "Provide a JSON object with a string question."); }
}
