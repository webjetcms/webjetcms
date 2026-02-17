package sk.iway.iwcm.system.spring.openapi;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

/**
 * REST controller for generating OpenAPI documentation.
 * Scans all @RestController beans with Spring MVC annotations and generates OpenAPI spec.
 *
 * Access URLs:
 * - JSON: /v3/api-docs
 * - YAML: /v3/api-docs.yaml
 * - Swagger UI: /admin/swagger-ui/index.html
 */
@RestController
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('users.edit_admins')")
public class OpenApiRestController {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private OpenAPI openApiInfo;

    private OpenAPI cachedOpenApi;
    private long lastCacheTime = 0;
    private static final long CACHE_DURATION_MS = 60000; // 1 minute cache

    /**
     * Returns OpenAPI specification in JSON format
     * @return OpenAPI JSON
     */
    @GetMapping(value = "/admin/rest/openapi/api-docs", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getOpenApiJson() {
        if (Constants.getBoolean("swaggerEnabled") == false) {
            //redirect to /404.jsp if swagger is not enabled
            Logger.debug(OpenApiRestController.class, "Swagger API docs requested but swaggerEnabled=false, returning 404");
            return "redirect:/404.jsp";
        }

        try {
            OpenAPI openApi = getOpenApi();
            return Json.pretty(openApi);
        } catch (Exception e) {
            Logger.error(OpenApiRestController.class, "Error generating OpenAPI JSON", e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * Generates or returns cached OpenAPI specification
     * @return OpenAPI object
     */
    private synchronized OpenAPI getOpenApi() {
        long now = System.currentTimeMillis();
        if (cachedOpenApi != null && (now - lastCacheTime) < CACHE_DURATION_MS) {
            return cachedOpenApi;
        }

        Logger.println(OpenApiRestController.class, "Generating OpenAPI specification...");

        // Clone the base OpenAPI info
        OpenAPI openApi = new OpenAPI()
                .info(openApiInfo.getInfo())
                .paths(new Paths());

        // Get all REST controller classes
        Set<Class<?>> resourceClasses = new HashSet<>();
        Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(RestController.class);

        for (Object controller : controllers.values()) {
            Class<?> clazz = controller.getClass();
            // Handle Spring proxies
            if (clazz.getName().contains("$$")) {
                clazz = clazz.getSuperclass();
            }
            resourceClasses.add(clazz);
        }

        Logger.println(OpenApiRestController.class, "Found " + resourceClasses.size() + " REST controllers");

        // Scan each controller
        for (Class<?> controllerClass : resourceClasses) {
            scanController(openApi, controllerClass);
        }

        cachedOpenApi = openApi;
        lastCacheTime = now;

        Logger.println(OpenApiRestController.class, "OpenAPI specification generated with " +
                (openApi.getPaths() != null ? openApi.getPaths().size() : 0) + " paths");

        return cachedOpenApi;
    }

    /**
     * Scans a controller class and adds paths to OpenAPI spec
     */
    private void scanController(OpenAPI openApi, Class<?> controllerClass) {
        String basePath = "";

        // Get base path from @RequestMapping on class (check hierarchy)
        Class<?> currentClass = controllerClass;
        while (currentClass != null && currentClass != Object.class) {
            RequestMapping classMapping = currentClass.getAnnotation(RequestMapping.class);
            if (classMapping != null) {
                if (classMapping.path().length > 0) {
                    basePath = classMapping.path()[0];
                    break;
                } else if (classMapping.value().length > 0) {
                    basePath = classMapping.value()[0];
                    break;
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        // Get tag from @Tag annotation
        String tagName = controllerClass.getSimpleName();
        Tag tagAnnotation = controllerClass.getAnnotation(Tag.class);
        if (tagAnnotation != null && Tools.isNotEmpty(tagAnnotation.name())) {
            tagName = tagAnnotation.name();
        }

        // Scan methods from entire class hierarchy
        Set<String> scannedMethods = new HashSet<>();
        currentClass = controllerClass;
        while (currentClass != null && currentClass != Object.class) {
            for (Method method : currentClass.getDeclaredMethods()) {
                // Create unique signature to avoid duplicates from overridden methods
                String methodSignature = getMethodSignature(method);
                if (!scannedMethods.contains(methodSignature)) {
                    scannedMethods.add(methodSignature);
                    scanMethod(openApi, method, basePath, tagName);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
    }

    /**
     * Creates a unique method signature for deduplication
     */
    private String getMethodSignature(Method method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getName()).append("(");
        for (Class<?> paramType : method.getParameterTypes()) {
            sb.append(paramType.getName()).append(",");
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Scans a method and adds operation to OpenAPI spec
     */
    private void scanMethod(OpenAPI openApi, Method method, String basePath, String tagName) {
        String path = basePath;
        RequestMethod[] httpMethods = null;

        // Check for mapping annotations
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        PutMapping putMapping = method.getAnnotation(PutMapping.class);
        DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
        PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);

        if (getMapping != null) {
            path = combinePath(basePath, getMapping.value().length > 0 ? getMapping.value()[0] : (getMapping.path().length > 0 ? getMapping.path()[0] : ""));
            httpMethods = new RequestMethod[]{RequestMethod.GET};
        } else if (postMapping != null) {
            path = combinePath(basePath, postMapping.value().length > 0 ? postMapping.value()[0] : (postMapping.path().length > 0 ? postMapping.path()[0] : ""));
            httpMethods = new RequestMethod[]{RequestMethod.POST};
        } else if (putMapping != null) {
            path = combinePath(basePath, putMapping.value().length > 0 ? putMapping.value()[0] : (putMapping.path().length > 0 ? putMapping.path()[0] : ""));
            httpMethods = new RequestMethod[]{RequestMethod.PUT};
        } else if (deleteMapping != null) {
            path = combinePath(basePath, deleteMapping.value().length > 0 ? deleteMapping.value()[0] : (deleteMapping.path().length > 0 ? deleteMapping.path()[0] : ""));
            httpMethods = new RequestMethod[]{RequestMethod.DELETE};
        } else if (patchMapping != null) {
            path = combinePath(basePath, patchMapping.value().length > 0 ? patchMapping.value()[0] : (patchMapping.path().length > 0 ? patchMapping.path()[0] : ""));
            httpMethods = new RequestMethod[]{RequestMethod.PATCH};
        } else if (requestMapping != null) {
            path = combinePath(basePath, requestMapping.value().length > 0 ? requestMapping.value()[0] : (requestMapping.path().length > 0 ? requestMapping.path()[0] : ""));
            httpMethods = requestMapping.method().length > 0 ? requestMapping.method() : new RequestMethod[]{RequestMethod.GET};
        }

        if (httpMethods == null) {
            return; // Not a mapped method
        }

        // Create operation
        io.swagger.v3.oas.models.Operation operation = new io.swagger.v3.oas.models.Operation();
        operation.addTagsItem(tagName);

        // Get operation info from @Operation annotation
        Operation opAnnotation = method.getAnnotation(Operation.class);
        if (opAnnotation != null) {
            if (Tools.isNotEmpty(opAnnotation.summary())) {
                operation.setSummary(opAnnotation.summary());
            }
            if (Tools.isNotEmpty(opAnnotation.description())) {
                operation.setDescription(opAnnotation.description());
            }
            if (Tools.isNotEmpty(opAnnotation.operationId())) {
                operation.setOperationId(opAnnotation.operationId());
            }
        }

        if (operation.getOperationId() == null) {
            operation.setOperationId(method.getName());
        }

        // Add parameters
        for (Parameter param : method.getParameters()) {
            io.swagger.v3.oas.models.parameters.Parameter oasParam = createParameter(param);
            if (oasParam != null) {
                operation.addParametersItem(oasParam);
            }

            // Check for request body
            if (param.isAnnotationPresent(RequestBody.class)) {
                io.swagger.v3.oas.models.parameters.RequestBody oasRequestBody = new io.swagger.v3.oas.models.parameters.RequestBody();
                oasRequestBody.setContent(new Content().addMediaType("application/json",
                        new io.swagger.v3.oas.models.media.MediaType().schema(new Schema<>().type("object"))));
                operation.setRequestBody(oasRequestBody);
            }
        }

        // Add default response
        ApiResponses responses = new ApiResponses();
        responses.addApiResponse("200", new ApiResponse().description("Successful operation"));
        operation.setResponses(responses);

        // Get or create path item
        PathItem pathItem = openApi.getPaths().get(path);
        if (pathItem == null) {
            pathItem = new PathItem();
            openApi.getPaths().addPathItem(path, pathItem);
        }

        // Add operation to path item
        for (RequestMethod httpMethod : httpMethods) {
            switch (httpMethod) {
                case GET:
                    pathItem.setGet(operation);
                    break;
                case POST:
                    pathItem.setPost(operation);
                    break;
                case PUT:
                    pathItem.setPut(operation);
                    break;
                case DELETE:
                    pathItem.setDelete(operation);
                    break;
                case PATCH:
                    pathItem.setPatch(operation);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Creates an OpenAPI parameter from a method parameter
     */
    private io.swagger.v3.oas.models.parameters.Parameter createParameter(Parameter param) {
        PathVariable pathVar = param.getAnnotation(PathVariable.class);
        RequestParam requestParam = param.getAnnotation(RequestParam.class);

        if (pathVar != null) {
            io.swagger.v3.oas.models.parameters.PathParameter pathParameter = new io.swagger.v3.oas.models.parameters.PathParameter();
            pathParameter.setName(Tools.isNotEmpty(pathVar.value()) ? pathVar.value() : param.getName());
            pathParameter.setSchema(getSchemaForType(param.getType()));
            pathParameter.setRequired(pathVar.required());

            // Check for @Parameter annotation
            io.swagger.v3.oas.annotations.Parameter paramAnnotation = param.getAnnotation(io.swagger.v3.oas.annotations.Parameter.class);
            if (paramAnnotation != null && Tools.isNotEmpty(paramAnnotation.description())) {
                pathParameter.setDescription(paramAnnotation.description());
            }

            return pathParameter;
        } else if (requestParam != null) {
            io.swagger.v3.oas.models.parameters.QueryParameter queryParameter = new io.swagger.v3.oas.models.parameters.QueryParameter();
            queryParameter.setName(Tools.isNotEmpty(requestParam.value()) ? requestParam.value() : (Tools.isNotEmpty(requestParam.name()) ? requestParam.name() : param.getName()));
            queryParameter.setSchema(getSchemaForType(param.getType()));
            queryParameter.setRequired(requestParam.required());

            // Check for @Parameter annotation
            io.swagger.v3.oas.annotations.Parameter paramAnnotation = param.getAnnotation(io.swagger.v3.oas.annotations.Parameter.class);
            if (paramAnnotation != null && Tools.isNotEmpty(paramAnnotation.description())) {
                queryParameter.setDescription(paramAnnotation.description());
            }

            return queryParameter;
        }

        return null;
    }

    /**
     * Gets OpenAPI schema for a Java type
     */
    @SuppressWarnings("rawtypes")
    private Schema getSchemaForType(Class<?> type) {
        Schema schema = new Schema<>();
        if (type == String.class) {
            schema.setType("string");
        } else if (type == Integer.class || type == int.class) {
            schema.setType("integer");
            schema.setFormat("int32");
        } else if (type == Long.class || type == long.class) {
            schema.setType("integer");
            schema.setFormat("int64");
        } else if (type == Boolean.class || type == boolean.class) {
            schema.setType("boolean");
        } else if (type == Double.class || type == double.class || type == Float.class || type == float.class) {
            schema.setType("number");
        } else {
            schema.setType("object");
        }
        return schema;
    }

    /**
     * Combines base path with method path
     */
    private String combinePath(String basePath, String methodPath) {
        if (Tools.isEmpty(methodPath)) {
            return Tools.isEmpty(basePath) ? "/" : basePath;
        }
        if (Tools.isEmpty(basePath)) {
            return methodPath.startsWith("/") ? methodPath : "/" + methodPath;
        }
        if (basePath.endsWith("/") && methodPath.startsWith("/")) {
            return basePath + methodPath.substring(1);
        }
        if (!basePath.endsWith("/") && !methodPath.startsWith("/")) {
            return basePath + "/" + methodPath;
        }
        return basePath + methodPath;
    }

    /**
     * Clears the cached OpenAPI specification
     */
    public void clearCache() {
        cachedOpenApi = null;
        lastCacheTime = 0;
    }
}
