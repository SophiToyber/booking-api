package com.booking.exception;

import jakarta.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
    String sqlState = findSqlState(ex);
    if ("23503".equals(sqlState)) {
      // FK violation
      var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
          "Related resource not found (foreign key violation)");
      pd.setTitle("Not Found");
      return pd;
    }
    if ("23505".equals(sqlState)) {
      // Unique violation
      var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Unique constraint violation");
      pd.setTitle("Conflict");
      return pd;
    }
    var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Data integrity violation");
    pd.setTitle("Conflict");
    return pd;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
    var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
    pd.setTitle("Bad Request");
    pd.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.toMap(
            fe -> fe.getField(),
            fe -> fe.getDefaultMessage(),
            (a, b) -> a
        )));
    return pd;
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
    var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Constraint violation");
    pd.setTitle("Bad Request");
    return pd;
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ProblemDetail handleJsonParse(HttpMessageNotReadableException ex) {
    var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed JSON");
    pd.setTitle("Bad Request");
    return pd;
  }

  private String findSqlState(Throwable ex) {
    Throwable t = ex;
    while (t != null) {
      if (t instanceof SQLException se) {
        return se.getSQLState();
      }
      t = t.getCause();
    }
    return null;
  }
}
