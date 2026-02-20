package sk.iway.iwcm.system.spring;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableFieldError;
import sk.iway.iwcm.system.datatable.DatatableResponse;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.EditorException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
@ControllerAdvice(annotations = {Datatable.class})
public class DatatableExceptionHandlerV2
{
	@ResponseBody
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<DatatableResponse<Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
		DatatableResponse<Object> response = new DatatableResponse<>();

		List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
		List<ObjectError> globalErrors = ex.getBindingResult().getGlobalErrors();

		if (!fieldErrors.isEmpty()) {
			response.setFieldErrors(fieldErrors.stream().map(e -> new DatatableFieldError(getField(e.getField()), e.getDefaultMessage())).collect(Collectors.toList()));
		}

		if (!globalErrors.isEmpty()) {
			response.setError(globalErrors.stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining(", ")));
		}

		if (Tools.isEmpty(response.getError())) {
			//vyhod este globalnu error hlasku, aby sa zobrazila aj pri tlacitkach a user si preklikal taby na konkretne chyby
			response.setError(Prop.getInstance().getText("datatable.error.fieldErrorMessage"));
		}

		return ResponseEntity.ok(response);
	}

	private String getField(String str) {
		String result = str;

		if (str.contains(".")) {
			result = str.substring(str.indexOf(".") + 1);
		}

		return result;
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<DatatableResponse<Object>> handleException(ConstraintViolationException ex) {
		DatatableResponse<Object> response = new DatatableResponse<>();
		List<DatatableFieldError> errorsList = new ArrayList<>();

		if (ex.getConstraintViolations()!=null && ex.getConstraintViolations().isEmpty()==false) {
			for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
				String propertyName = violation.getPropertyPath().toString();
				int dot = propertyName.indexOf(".");
				//toto potrebujeme kvoli @Valid anotacii na nested properties, ktore su konvertovane, priklad je GroupsApproveEntity.group kde nam hadze chybu na group.navbarName ale DT ma definovany len field group
				if (dot > 0 && propertyName.startsWith("editorFields")==false) propertyName = propertyName.substring(0, dot);

				errorsList.add(new DatatableFieldError(propertyName, getErrorMessage(violation)));
			}
			response.setFieldErrors(errorsList);
		} else {
			response.setError(ex.getMessage());
			Logger.error(DatatableExceptionHandlerV2.class, "ConstraintViolationException: " + ex.getMessage());
		}

		if (DatatableRestControllerV2.getLastImportedRow()!=null) {
			StringBuilder errors = new StringBuilder("");
			if (response.getFieldErrors()!=null) {
				for (DatatableFieldError error : response.getFieldErrors()) {
					errors.append("\n").append(error.getName()).append(" - ").append(error.getStatus());
				}
			}
			if (Tools.isEmpty(response.getError())) {
				if (errors.length()<1) errors.append(Prop.getInstance().getText("datatable.error.fieldErrorMessage"));
				response.setError(Prop.getInstance().getText("datatable.error.importRow", String.valueOf(DatatableRestControllerV2.getLastImportedRow().intValue()+1), errors.toString()));
			} else {
				response.setError(Prop.getInstance().getText("datatable.error.importRow", String.valueOf(DatatableRestControllerV2.getLastImportedRow().intValue()+1), response.getError()));
			}
		}

		if (Tools.isEmpty(response.getError())) {
			//vyhod este globalnu error hlasku, aby sa zobrazila aj pri tlacitkach a user si preklikal taby na konkretne chyby
			response.setError(Prop.getInstance().getText("datatable.error.fieldErrorMessage"));
		}

		return new ResponseEntity<>(response, HttpStatus.OK);
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
						errorsList.add(new DatatableFieldError(violation.getPropertyPath().toString(), getErrorMessage(violation)));
					}
			}

			response.setFieldErrors(errorsList);
		} else if (ex instanceof TransactionSystemException) {
			String err = ex.getMessage();
			try {
				if (err != null) {
					int start = err.indexOf("Duplicate entry");
					if (start > 0) {
						int end = err.indexOf("Error Code", start);
						if (end > start) err = err.substring(start, end).trim();
					}
				}
			} catch (Exception e) {
				//failsafe
			}
			response.setError(err);
			Logger.error(DatatableExceptionHandlerV2.class, "TransactionSystemException: " + ex.getMessage());
		} else {
			response.setError(ex.getMessage());
			Logger.error(DatatableExceptionHandlerV2.class, "TransactionSystemException, exception: " + ex.getMessage(), ex);
		}

		if (Tools.isEmpty(response.getError())) {
			//vyhod este globalnu error hlasku, aby sa zobrazila aj pri tlacitkach a user si preklikal taby na konkretne chyby
			response.setError(Prop.getInstance().getText("datatable.error.fieldErrorMessage"));
		}

		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@ExceptionHandler(EditorException.class)
	public ResponseEntity<DatatableResponse<Object>> handleException(EditorException ex) {
		DatatableResponse<Object> response = new DatatableResponse<>();
		String message = null;

		message = prepareMessage(message, ex);

		response.setNotify(ex.getNotifyBeans());

		response.setError(message);
		Logger.error(DatatableExceptionHandlerV2.class, "EditorException: " + ex.getMessage());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<DatatableResponse<Object>> handleException(Exception ex) {
		DatatableResponse<Object> response = new DatatableResponse<>();
		String message = null;
		if (ex instanceof ResponseStatusException) {
			ResponseStatusException ex2 = (ResponseStatusException)ex;
			message = ex2.getReason();
		}

		message = prepareMessage(message, ex);

		response.setError(message);
		//log stack trace because this is unexpected exception
		Logger.error(DatatableExceptionHandlerV2.class, "handleException: " + ex.getMessage(), ex);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	private String prepareMessage(String message, Exception ex) {
		if (Tools.isEmpty(message)) message = ex.getMessage();
		if (Tools.isEmpty(message)) message = Prop.getInstance().getText("datatable.error.unknown");

		if (message!=null && message.contains("DatabaseException")) {
			int i = message.indexOf("Call:");
			if (i > 0) {
				Adminlog.add(Adminlog.TYPE_SQLERROR, message, -1, -1);
				message = message.substring(0, i).trim();
			}
		}
		if (message != null && message.contains("JSON parse error")) {
			int i = message.indexOf(", problem:");
			if (i > 0) {
				Adminlog.add(Adminlog.TYPE_JSPERROR, message, -1, -1);
				message = message.substring(0, i).trim();
			}
		}

		return message;
	}

	/**
	 * Pokusi sa ziskat text chyby z WJ prekladov
	 * @param violation
	 * @return
	 */
	private String getErrorMessage(ConstraintViolation<?> violation) {

		//{jakarta.validation.constraints.NotBlank.message}
		String key = violation.getMessageTemplate();
		if (key != null && key.length()>3) {
			if (key.startsWith("{") && key.endsWith("}") && key.length()>3) key = key.substring(1, key.length()-1);

			Prop prop = Prop.getInstance();
			String message = prop.getText(key);
			if (key.equals(message)==false) {
				//skus dohladat atributy a nahradit ich
				try {
					if (violation.getConstraintDescriptor()!=null && violation.getConstraintDescriptor().getAttributes()!=null) {
						Map<String, Object> attributes = violation.getConstraintDescriptor().getAttributes();
						for (Map.Entry<String, Object> entry : attributes.entrySet()) {
							String expression = "{"+entry.getKey()+"}";
							if (message.contains(expression)) {
								message = Tools.replace(message, expression, String.valueOf(attributes.get(entry.getKey())));
							}
						}
					}
					String validatedValue = String.valueOf(violation.getInvalidValue());
					message = Tools.replace(message, "${validatedValue}", validatedValue);

				} catch (Exception ex) {
					//failsafe
				}

				return message;
			}
		}

		return violation.getMessage();
	}

}
