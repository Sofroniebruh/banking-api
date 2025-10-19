package com.example.banking_api.config.exceptions;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class UserValidationException extends RuntimeException {
  private Map<String, String> fieldErrors;

  public UserValidationException(Map<String, String> fieldErrors) {
    super("errors");
    this.fieldErrors = fieldErrors;
  }

  public UserValidationException() {
    super("errors");
    this.fieldErrors = new HashMap<>();
  }

  public void addFieldError(String field, String error) {
    this.fieldErrors.put(field, error);
  }
}
