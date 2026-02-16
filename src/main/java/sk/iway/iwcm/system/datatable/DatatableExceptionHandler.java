package sk.iway.iwcm.system.datatable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Title        webjet8
 * Company      Interway a. s. (www.interway.sk)
 * Copyright    Interway a. s. (c) 2001-2019
 * @author       tmarcinkova $
 * @created      2019/05/10 11:05
 *
 *  Tato trieda spracuje vynimku a vrati response pre DataTable Editor
 *  	- stara sa len o vynimky z tried, ktore maju anotaciu '@Datatable'
 *
 */
@ControllerAdvice(annotations = Datatable.class)
public class DatatableExceptionHandler
{
	 @ExceptionHandler(ConstraintViolationException.class)
	 public ResponseEntity<DatatableResponse<Object>> handleException(ConstraintViolationException ex) {
		  DatatableResponse<Object> response = new DatatableResponse<>();
		  List<DatatableFieldError> errorsList = new ArrayList<>();

		  if (!ex.getConstraintViolations().isEmpty()) {
				for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
					 errorsList.add(new DatatableFieldError(violation.getPropertyPath().toString(), violation.getMessage()));
				}
				response.setFieldErrors(errorsList);
		  } else {
				response.setError(ex.getMessage());
		  }

		  return new ResponseEntity<>(response, null, HttpStatus.OK);
	 }

	 @ExceptionHandler(TransactionSystemException.class)
	 public ResponseEntity<DatatableResponse<Object>> handleException(TransactionSystemException ex) {
		  DatatableResponse<Object> response = new DatatableResponse<>();
		  List<DatatableFieldError> errorsList = new ArrayList<>();

		  Throwable t = ex.getCause();
		  while ((t != null) && !(t instanceof ConstraintViolationException)) {
				t = t.getCause();
		  }
		  if (t instanceof ConstraintViolationException) {
				// Here you're sure you have a ConstraintViolationException, you can handle it
				Set<ConstraintViolation<?>> violations = ((ConstraintViolationException) t).getConstraintViolations();
				if (!violations.isEmpty()) {
					 for (ConstraintViolation<?> violation : violations) {
						  errorsList.add(new DatatableFieldError(violation.getPropertyPath().toString(), violation.getMessage()));
					 }
				}
		  } else {
				response.setError(ex.getMessage());
		  }

		  return new ResponseEntity<>(response, null, HttpStatus.OK);
	 }

	 @ExceptionHandler(Exception.class)
	 public ResponseEntity<DatatableResponse<Object>> handleException(Exception ex) {
		  DatatableResponse<Object> response = new DatatableResponse<>();
		  response.setError("exception: " + ex.getMessage());
		  return new ResponseEntity<>(response, null, HttpStatus.OK);
	 }
}
