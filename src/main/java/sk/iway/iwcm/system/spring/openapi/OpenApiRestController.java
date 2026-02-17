package sk.iway.iwcm.system.spring.openapi;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
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
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ArraySchema;
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
 * - JSON: /admin/rest/openapi/api-docs
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
    private static final long CACHE_DURATION_MS = 60 * 60 * 1000; // 1 hour cache

    // Track schemas to be generated
    @SuppressWarnings("rawtypes")
    private Map<String, Schema> schemasToGenerate;
    private Set<Class<?>> processedClasses;

    /**
     * Returns OpenAPI specification in JSON format
     * @return OpenAPI JSON
     */
    @GetMapping(value = "/admin/rest/openapi/api-docs", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getOpenApiJson() {
        if (Constants.getBoolean("swaggerEnabled") == false) {
            //redirect to /404.jsp if swagger is not enabled
            Logger.debug(OpenApiRestController.class, "Swagger API docs requested but swaggerEnabled=false, returning 404");
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
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

        // Initialize schema tracking
        schemasToGenerate = new HashMap<>();
        processedClasses = new HashSet<>();

        // Clone the base OpenAPI info
        OpenAPI openApi = new OpenAPI()
                .info(openApiInfo.getInfo())
                .paths(new Paths())
                .components(new Components());

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

        // Add all collected schemas to components
        if (!schemasToGenerate.isEmpty()) {
            openApi.getComponents().setSchemas(schemasToGenerate);
        }

        cachedOpenApi = openApi;
        lastCacheTime = now;

        Logger.println(OpenApiRestController.class, "OpenAPI specification generated with " +
                (openApi.getPaths() != null ? openApi.getPaths().size() : 0) + " paths and " +
                schemasToGenerate.size() + " schemas");

        return cachedOpenApi;
    }

    /**
     * Scans a controller class and adds paths to OpenAPI spec
     */
    private void scanController(OpenAPI openApi, Class<?> controllerClass) {
        String basePath = "";

        // Build type variable mappings from the entire class hierarchy
        Map<TypeVariable<?>, Type> typeVarMappings = resolveTypeVariables(controllerClass);

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
                    scanMethod(openApi, method, basePath, tagName, typeVarMappings);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
    }

    /**
     * Resolves type variable mappings from the class hierarchy.
     * For example, if A extends B&lt;String&gt; and B&lt;T&gt; extends C&lt;T, Integer&gt;,
     * this will map T -> String for B's type parameters.
     */
    private Map<TypeVariable<?>, Type> resolveTypeVariables(Class<?> leafClass) {
        Map<TypeVariable<?>, Type> mappings = new HashMap<>();

        Class<?> currentClass = leafClass;
        while (currentClass != null && currentClass != Object.class) {
            Type genericSuperclass = currentClass.getGenericSuperclass();
            if (genericSuperclass instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) genericSuperclass;
                Class<?> rawType = (Class<?>) paramType.getRawType();
                TypeVariable<?>[] typeParams = rawType.getTypeParameters();
                Type[] actualArgs = paramType.getActualTypeArguments();

                for (int i = 0; i < typeParams.length && i < actualArgs.length; i++) {
                    Type resolvedType = actualArgs[i];
                    // If the actual arg is itself a type variable, try to resolve it
                    if (resolvedType instanceof TypeVariable) {
                        Type mapped = mappings.get(resolvedType);
                        if (mapped != null) {
                            resolvedType = mapped;
                        }
                    }
                    mappings.put(typeParams[i], resolvedType);
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        return mappings;
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
    private void scanMethod(OpenAPI openApi, Method method, String basePath, String tagName, Map<TypeVariable<?>, Type> typeVarMappings) {
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

            // Resolve the parameter type considering generic type mappings
            Type resolvedGenericType = resolveType(param.getParameterizedType(), typeVarMappings);
            Class<?> resolvedType = getClassFromType(resolvedGenericType);

            // Check for request body (explicit @RequestBody annotation)
            if (param.isAnnotationPresent(RequestBody.class)) {
                io.swagger.v3.oas.models.parameters.RequestBody oasRequestBody = new io.swagger.v3.oas.models.parameters.RequestBody();
                Schema<?> bodySchema = getOrCreateSchema(resolvedType, resolvedGenericType, typeVarMappings);
                oasRequestBody.setContent(new Content().addMediaType("application/json",
                        new io.swagger.v3.oas.models.media.MediaType().schema(bodySchema)));
                operation.setRequestBody(oasRequestBody);
            }
            // Check for implicit request body - complex types without @PathVariable, @RequestParam, @RequestBody
            // These are typically form parameters that Spring binds automatically
            else if (oasParam == null && !param.isAnnotationPresent(RequestBody.class) && isComplexType(resolvedType)) {
                io.swagger.v3.oas.models.parameters.RequestBody oasRequestBody = new io.swagger.v3.oas.models.parameters.RequestBody();
                Schema<?> bodySchema = getOrCreateSchema(resolvedType, resolvedGenericType, typeVarMappings);
                // For form data binding, use form-urlencoded and multipart
                Content content = new Content();
                content.addMediaType("application/x-www-form-urlencoded",
                        new io.swagger.v3.oas.models.media.MediaType().schema(bodySchema));
                content.addMediaType("multipart/form-data",
                        new io.swagger.v3.oas.models.media.MediaType().schema(bodySchema));
                oasRequestBody.setContent(content);
                operation.setRequestBody(oasRequestBody);
            }
        }

        // Add response with proper schema based on return type
        ApiResponses responses = new ApiResponses();
        ApiResponse successResponse = new ApiResponse().description("Successful operation");

        Type resolvedReturnType = resolveType(method.getGenericReturnType(), typeVarMappings);
        // Unwrap wrapper types like ResponseEntity, Optional, etc.
        resolvedReturnType = unwrapResponseType(resolvedReturnType, typeVarMappings);
        Class<?> returnType = getClassFromType(resolvedReturnType);
        if (returnType != void.class && returnType != Void.class) {
            Schema<?> responseSchema = getOrCreateSchema(returnType, resolvedReturnType, typeVarMappings);
            successResponse.setContent(new Content().addMediaType("application/json",
                    new io.swagger.v3.oas.models.media.MediaType().schema(responseSchema)));
        }
        responses.addApiResponse("200", successResponse);
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
     * Resolves a Type by replacing TypeVariables with their actual types from the mappings
     */
    private Type resolveType(Type type, Map<TypeVariable<?>, Type> typeVarMappings) {
        if (type instanceof TypeVariable) {
            Type resolved = typeVarMappings.get(type);
            return resolved != null ? resolved : type;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Type[] actualArgs = pType.getActualTypeArguments();
            Type[] resolvedArgs = new Type[actualArgs.length];
            boolean changed = false;
            for (int i = 0; i < actualArgs.length; i++) {
                resolvedArgs[i] = resolveType(actualArgs[i], typeVarMappings);
                if (resolvedArgs[i] != actualArgs[i]) {
                    changed = true;
                }
            }
            if (changed) {
                return new ResolvedParameterizedType(pType.getRawType(), resolvedArgs, pType.getOwnerType());
            }
        }
        return type;
    }

    /**
     * Extracts the raw Class from a Type
     */
    private Class<?> getClassFromType(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        }
        if (type instanceof TypeVariable) {
            // Unresolved type variable, default to Object
            return Object.class;
        }
        return Object.class;
    }

    /**
     * Unwraps wrapper response types like ResponseEntity, Optional, HttpEntity
     * to get the actual payload type.
     * For example: ResponseEntity&lt;CategoryDto&gt; -&gt; CategoryDto
     */
    private Type unwrapResponseType(Type type, Map<TypeVariable<?>, Type> typeVarMappings) {
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Class<?> rawClass = (Class<?>) pType.getRawType();

            // Check if it's a wrapper type that should be unwrapped
            String className = rawClass.getName();
            if (className.equals("org.springframework.http.ResponseEntity") ||
                className.equals("org.springframework.http.HttpEntity") ||
                className.equals("java.util.Optional") ||
                className.equals("java.util.concurrent.CompletableFuture") ||
                className.equals("reactor.core.publisher.Mono")) {

                Type[] typeArgs = pType.getActualTypeArguments();
                if (typeArgs.length > 0) {
                    Type innerType = typeArgs[0];
                    // Resolve type variable if needed
                    innerType = resolveType(innerType, typeVarMappings);
                    // Recursively unwrap in case of nested wrappers
                    return unwrapResponseType(innerType, typeVarMappings);
                }
            }
        }
        return type;
    }

    /**
     * Simple implementation of ParameterizedType for resolved types
     */
    private static class ResolvedParameterizedType implements ParameterizedType {
        private final Type rawType;
        private final Type[] actualTypeArguments;
        private final Type ownerType;

        ResolvedParameterizedType(Type rawType, Type[] actualTypeArguments, Type ownerType) {
            this.rawType = rawType;
            this.actualTypeArguments = actualTypeArguments;
            this.ownerType = ownerType;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return actualTypeArguments;
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public Type getOwnerType() {
            return ownerType;
        }
    }

    /**
     * Gets or creates an OpenAPI schema for a type, registering complex types for later generation
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Schema<?> getOrCreateSchema(Class<?> type, Type genericType, Map<TypeVariable<?>, Type> typeVarMappings) {
        // Handle primitive and simple types
        Schema simpleSchema = getSimpleSchemaForType(type);
        if (simpleSchema != null) {
            return simpleSchema;
        }

        // Handle arrays
        if (type.isArray()) {
            ArraySchema arraySchema = new ArraySchema();
            arraySchema.setItems(getOrCreateSchema(type.getComponentType(), type.getComponentType(), typeVarMappings));
            return arraySchema;
        }

        // Handle collections (List, Set, etc.)
        if (Collection.class.isAssignableFrom(type)) {
            ArraySchema arraySchema = new ArraySchema();
            if (genericType instanceof ParameterizedType) {
                Type[] typeArgs = ((ParameterizedType) genericType).getActualTypeArguments();
                if (typeArgs.length > 0) {
                    Type resolvedItemType = resolveType(typeArgs[0], typeVarMappings);
                    Class<?> itemClass = getClassFromType(resolvedItemType);
                    arraySchema.setItems(getOrCreateSchema(itemClass, resolvedItemType, typeVarMappings));
                } else {
                    arraySchema.setItems(new Schema<>().type("object"));
                }
            } else {
                arraySchema.setItems(new Schema<>().type("object"));
            }
            return arraySchema;
        }

        // Handle Map
        if (Map.class.isAssignableFrom(type)) {
            Schema mapSchema = new Schema<>();
            mapSchema.setType("object");
            mapSchema.setAdditionalProperties(new Schema<>().type("object"));
            return mapSchema;
        }

        // Complex type - create reference and generate schema if not already done
        String schemaName = type.getSimpleName();
        if (!schemasToGenerate.containsKey(schemaName) && !processedClasses.contains(type)) {
            // Add to processedClasses BEFORE calling generateSchemaForClass to prevent
            // infinite recursion with circular references (e.g., A references B, B references A)
            processedClasses.add(type);
            generateSchemaForClass(type);
        }

        // Return a reference to the schema
        Schema refSchema = new Schema<>();
        refSchema.set$ref("#/components/schemas/" + schemaName);
        return refSchema;
    }

    /**
     * Generates a schema for a complex class type
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void generateSchemaForClass(Class<?> clazz) {
        if (processedClasses.contains(clazz)) {
            return;
        }
        processedClasses.add(clazz);

        String schemaName = clazz.getSimpleName();
        Schema schema = new Schema<>();
        schema.setType("object");
        schema.setDescription(clazz.getName());

        // Check for @Schema annotation on class
        io.swagger.v3.oas.annotations.media.Schema classSchemaAnn = clazz.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);
        if (classSchemaAnn != null && Tools.isNotEmpty(classSchemaAnn.description())) {
            schema.setDescription(classSchemaAnn.description());
        }

        Map<String, Schema> properties = new HashMap<>();

        // Get all fields from class hierarchy
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                // Skip static, transient and synthetic fields
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) ||
                    java.lang.reflect.Modifier.isTransient(field.getModifiers()) ||
                    field.isSynthetic()) {
                    continue;
                }

                String fieldName = field.getName();
                Schema fieldSchema = createFieldSchema(field);
                if (fieldSchema != null) {
                    properties.put(fieldName, fieldSchema);
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        if (!properties.isEmpty()) {
            schema.setProperties(properties);
        }

        schemasToGenerate.put(schemaName, schema);
    }

    /**
     * Creates a schema for a field
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Schema createFieldSchema(Field field) {
        Class<?> fieldType = field.getType();
        Type genericType = field.getGenericType();

        // Check for @Schema annotation
        io.swagger.v3.oas.annotations.media.Schema schemaAnnotation = field.getAnnotation(io.swagger.v3.oas.annotations.media.Schema.class);

        Schema schema;

        // Handle simple types
        Schema simpleSchema = getSimpleSchemaForType(fieldType);
        if (simpleSchema != null) {
            schema = simpleSchema;
        }
        // Handle arrays
        else if (fieldType.isArray()) {
            ArraySchema arraySchema = new ArraySchema();
            Schema itemSchema = getSimpleSchemaForType(fieldType.getComponentType());
            if (itemSchema != null) {
                arraySchema.setItems(itemSchema);
            } else {
                // Complex array item type - use reference
                String itemSchemaName = fieldType.getComponentType().getSimpleName();
                if (!processedClasses.contains(fieldType.getComponentType())) {
                    generateSchemaForClass(fieldType.getComponentType());
                }
                Schema refSchema = new Schema<>();
                refSchema.set$ref("#/components/schemas/" + itemSchemaName);
                arraySchema.setItems(refSchema);
            }
            schema = arraySchema;
        }
        // Handle collections
        else if (Collection.class.isAssignableFrom(fieldType)) {
            ArraySchema arraySchema = new ArraySchema();
            if (genericType instanceof ParameterizedType) {
                Type[] typeArgs = ((ParameterizedType) genericType).getActualTypeArguments();
                if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                    Class<?> itemClass = (Class<?>) typeArgs[0];
                    Schema itemSchema = getSimpleSchemaForType(itemClass);
                    if (itemSchema != null) {
                        arraySchema.setItems(itemSchema);
                    } else {
                        String itemSchemaName = itemClass.getSimpleName();
                        if (!processedClasses.contains(itemClass)) {
                            generateSchemaForClass(itemClass);
                        }
                        Schema refSchema = new Schema<>();
                        refSchema.set$ref("#/components/schemas/" + itemSchemaName);
                        arraySchema.setItems(refSchema);
                    }
                } else {
                    arraySchema.setItems(new Schema<>().type("object"));
                }
            } else {
                arraySchema.setItems(new Schema<>().type("object"));
            }
            schema = arraySchema;
        }
        // Handle Map
        else if (Map.class.isAssignableFrom(fieldType)) {
            schema = new Schema<>();
            schema.setType("object");
            schema.setAdditionalProperties(new Schema<>().type("object"));
        }
        // Handle enums
        else if (fieldType.isEnum()) {
            schema = new Schema<>();
            schema.setType("string");
            Object[] enumConstants = fieldType.getEnumConstants();
            for (Object enumConstant : enumConstants) {
                schema.addEnumItemObject(enumConstant.toString());
            }
        }
        // Complex type - use reference
        else {
            String refSchemaName = fieldType.getSimpleName();
            if (!processedClasses.contains(fieldType)) {
                generateSchemaForClass(fieldType);
            }
            schema = new Schema<>();
            schema.set$ref("#/components/schemas/" + refSchemaName);
        }

        // Apply @Schema annotation properties
        if (schemaAnnotation != null) {
            if (Tools.isNotEmpty(schemaAnnotation.description())) {
                schema.setDescription(schemaAnnotation.description());
            }
            if (Tools.isNotEmpty(schemaAnnotation.example())) {
                schema.setExample(schemaAnnotation.example());
            }
        }

        return schema;
    }

    /**
     * Gets OpenAPI schema for simple/primitive types, returns null for complex types
     */
    @SuppressWarnings("rawtypes")
    private Schema getSimpleSchemaForType(Class<?> type) {
        Schema schema = new Schema<>();

        if (type == String.class || type == CharSequence.class) {
            schema.setType("string");
        } else if (type == Integer.class || type == int.class) {
            schema.setType("integer");
            schema.setFormat("int32");
        } else if (type == Long.class || type == long.class) {
            schema.setType("integer");
            schema.setFormat("int64");
        } else if (type == Short.class || type == short.class) {
            schema.setType("integer");
            schema.setFormat("int32");
        } else if (type == Byte.class || type == byte.class) {
            schema.setType("integer");
            schema.setFormat("int32");
        } else if (type == Boolean.class || type == boolean.class) {
            schema.setType("boolean");
        } else if (type == Double.class || type == double.class) {
            schema.setType("number");
            schema.setFormat("double");
        } else if (type == Float.class || type == float.class) {
            schema.setType("number");
            schema.setFormat("float");
        } else if (type == BigDecimal.class) {
            schema.setType("number");
        } else if (type == Date.class || type.getName().startsWith("java.time.")) {
            schema.setType("string");
            schema.setFormat("date-time");
        } else if (type == Character.class || type == char.class) {
            schema.setType("string");
        } else {
            // Not a simple type
            return null;
        }

        return schema;
    }

    /**
     * Gets OpenAPI schema for a Java type (for parameters - simple types only)
     */
    @SuppressWarnings("rawtypes")
    private Schema getSchemaForType(Class<?> type) {
        Schema simpleSchema = getSimpleSchemaForType(type);
        if (simpleSchema != null) {
            return simpleSchema;
        }
        // For parameters, just return object type for complex types
        Schema schema = new Schema<>();
        schema.setType("object");
        return schema;
    }

    /**
     * Determines if a type is a complex type that should be treated as a request body.
     * Complex types are not primitives, not wrappers, not String, not common JDK types.
     */
    private boolean isComplexType(Class<?> type) {
        if (type.isPrimitive()) return false;
        if (type.isArray()) return false;
        if (type.isEnum()) return false;
        if (type == String.class) return false;
        if (Number.class.isAssignableFrom(type)) return false;
        if (type == Boolean.class) return false;
        if (type == Character.class) return false;
        if (type == Date.class) return false;
        if (type.getName().startsWith("java.time.")) return false;
        if (Collection.class.isAssignableFrom(type)) return false;
        if (Map.class.isAssignableFrom(type)) return false;
        if (type == Object.class) return false;
        // Spring framework classes
        if (type.getName().startsWith("org.springframework.")) return false;
        if (type.getName().startsWith("jakarta.servlet.")) return false;
        // It's a complex domain type
        return true;
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
