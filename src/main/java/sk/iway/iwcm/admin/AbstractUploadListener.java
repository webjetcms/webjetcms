package sk.iway.iwcm.admin;

import org.springframework.beans.BeanUtils;
import org.springframework.core.GenericTypeResolver;
import org.springframework.ui.ModelMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.AbstractMultipartHttpServletRequest;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.spring.events.WebjetEvent;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Abstraktna trieda ktora spracuje multipart upload v MVC aplikacii vo WebJET CMS
 * Vysledkom je vrateny objekt form aj s pripojenym suborom
 */
public abstract class AbstractUploadListener<T> {

    private final Validator validator;
    private BindingResult bindingResult;
    private boolean isPost;
    private T form;

    protected AbstractUploadListener(Validator validator) {
        this.validator = validator;
    }

    /**
     * Metoda pre sprocesovanie formularu a validovanie
     * @param event WebjetEvent<ThymeleafEvent>
     */
    public void processForm(final WebjetEvent<ThymeleafEvent> event) {
        ModelMap model = event.getSource().getModel();
        this.isPost = "post".equalsIgnoreCase(event.getSource().getRequest().getMethod());

        Optional<T> formOptional = getBeanOptional(event.getSource().getRequest());
        if (!formOptional.isPresent()) {
            return;
        }

        this.form = formOptional.get();
        this.bindingResult = new BeanPropertyBindingResult(form, "form");

        validate(model, form);
    }

    /**
     * Metoda pre validaciu dat z formularu
     * @param model ModelMap
     * @param form T
     */
    public void validate(ModelMap model, T form) {
        if (!isPost) {
            return;
        }

        Set<ConstraintViolation<T>> violations = validator.validate(form);

        //we dont know why, but validator for files returns same error two times
        Set<String> duplicityCheck = new HashSet<>();

        for (ConstraintViolation<T> violation : violations) {
            String message = violation.getMessage();
            String path = violation.getPropertyPath().toString();
            String key = path + "_" + message;
            if (duplicityCheck.contains(key)) {
                continue;
            }
            duplicityCheck.add(key);
            this.bindingResult.rejectValue(path, "", message);
        }

        model.put(BindingResult.MODEL_KEY_PREFIX + "form", this.bindingResult);
    }

    /**
     * Pomocna metoda pre ziskanie informacie, ci je request typu post
     * @return boolean
     */
    public boolean isPost() {
        return isPost;
    }

    /**
     * Pomocna metoda pre ziskanie vysledku validacii
     * @return BindingResult
     */
    public BindingResult getBindingResult() {
        return bindingResult;
    }

    /**
     * Pomocna metoda pre ziskanie objektu s vyplnenymi datami vo formulari
     * @return T
     */
    public T getForm() {
        return form;
    }

    /**
     * Metoda pre ziskanie dat z requestu a nasetovanie do generickeho objektu
     * @param request HttpServletRequest
     * @return Optional<T>
     */
    private Optional<T> getBeanOptional(HttpServletRequest request) {
        //noinspection unchecked
        @SuppressWarnings("unchecked")
        Class<T> tClass = (Class<T>) GenericTypeResolver.resolveTypeArgument(getClass(), AbstractUploadListener.class);
        if (tClass == null) {
            Logger.error(AbstractUploadListener.class, "Cannot resolve type argument of this class: {}", getClass());
            return Optional.empty();
        }

        T instance = BeanUtils.instantiateClass(tClass);
        if (!isPost) {
            return Optional.of(instance);
        }

        setParametersToProperties(request, instance);
        setMultipartFilePerametersToProperties(request, instance);

        return Optional.of(instance);
    }

    /**
     * Metoda pre nasavenie hodnot z formularu do properties objektu
     * @param request HttpServletRequest
     * @param form T
     */
    private void setParametersToProperties(HttpServletRequest request, T form) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Map.Entry<String, String[]> stringEntry : parameterMap.entrySet()) {
            setProperty(request, form, stringEntry.getKey());
        }
    }

    /**
     * Metoda pre nasavenie hodnoty z formularu do property objektu
     * @param request HttpServletRequest
     * @param form T
     * @param key String
     */
    private void setProperty(HttpServletRequest request, T form, String key) {
        Method declaredMethodWithMinimalParameters = BeanUtils.findDeclaredMethodWithMinimalParameters(form.getClass(), "set" + StringUtils.capitalize(key));
        if (declaredMethodWithMinimalParameters == null) {
            return;
        }

        try {
            declaredMethodWithMinimalParameters.invoke(form, Tools.getParameter(request, key));
        } catch (IllegalAccessException | InvocationTargetException e) {
            sk.iway.iwcm.Logger.error(e);
        }
    }

    /**
     * Metoda pre vlozenie suborov z formularu do properties objektu
     * @param request HttpServletRequest
     * @param form T
     */
    private void setMultipartFilePerametersToProperties(HttpServletRequest request, T form) {
        if (request instanceof AbstractMultipartHttpServletRequest req) {
            MultiValueMap<String, MultipartFile> multiFileMap = req.getMultiFileMap();
            for (Map.Entry<String, List<MultipartFile>> stringListEntry : multiFileMap.entrySet()) {
                String key = stringListEntry.getKey();
                List<MultipartFile> value = stringListEntry.getValue();

                for (MultipartFile multipartFile : value) {
                    if (multipartFile.getSize() == 0) {
                        continue;
                    }

                    Method declaredMethodWithMinimalParameters = BeanUtils.findDeclaredMethod(form.getClass(), "set" + StringUtils.capitalize(key), MultipartFile.class);
                    if (declaredMethodWithMinimalParameters == null) {
                        continue;
                    }

                    try {
                        declaredMethodWithMinimalParameters.invoke(form, multipartFile);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        sk.iway.iwcm.Logger.error(e);
                    }
                }
            }
        }
    }
}