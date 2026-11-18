/** Renders local JSON Schema references as lazily expandable fields without changing the wire schema. */
function renderAiToolSchema(container, root, labels) {
    const element = (tag, className, text) => {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text != null) node.textContent = text;
        return node;
    };

    function resolve(schema) {
        const references = new Set();
        while (schema?.$ref?.startsWith("#/") && !references.has(schema.$ref)) {
            references.add(schema.$ref);
            const target = schema.$ref.substring(2).split("/").reduce((value, key) =>
                value?.[key.replace(/~1/g, "/").replace(/~0/g, "~")], root);
            if (!target) break;
            const { $ref, ...overrides } = schema;
            schema = { ...target, ...overrides };
        }
        return schema;
    }

    function model(schema) {
        schema = resolve(schema);
        if (!schema || typeof schema !== "object") return schema;
        // Nullable response objects still have one model to explore.
        const variants = schema.anyOf || schema.oneOf;
        if (variants?.length === 2) {
            const choices = variants.map(resolve);
            const value = choices.find(choice => choice.type !== "null");
            if (value && choices.some(choice => choice.type === "null")) {
                const { anyOf, oneOf, ...metadata } = schema;
                const types = [].concat(value.type || "object");
                return { ...value, ...metadata, type: [...new Set([...types, "null"])] };
            }
        }
        return schema;
    }

    function children(schema, path) {
        if (!schema || typeof schema !== "object") return [];
        const required = schema.required || [];
        const fields = Object.entries(schema.properties || {}).map(([name, value]) => ({
            name, schema: value, path: path ? path + "." + name : name, required: required.includes(name)
        }));
        fields.sort((a, b) => Number(b.required) - Number(a.required));
        if (schema.items) fields.push({ name: "[]", schema: schema.items, path: path + "[]" });
        for (const [pattern, value] of Object.entries(schema.patternProperties || {})) {
            fields.push({ name: pattern, schema: value, path: path + ".*" });
        }
        if (schema.additionalProperties && typeof schema.additionalProperties === "object") {
            fields.push({ name: "*", schema: schema.additionalProperties, path: path + ".*" });
        }
        for (const keyword of ["anyOf", "oneOf", "allOf"]) {
            for (const [index, value] of (schema[keyword] || []).entries()) {
                fields.push({ name: keyword + " " + (index + 1), schema: value, path });
            }
        }
        return fields;
    }

    function field({ name, schema, path, required }, depth) {
        schema = model(schema);
        const fields = children(schema, path);
        const node = element(fields.length ? "details" : "div", "ai-tool-schema-field");
        node.dataset.property = path;
        const heading = element(fields.length ? "summary" : "div", "ai-tool-schema-heading");
        if (name) heading.append(element("code", "ai-tool-schema-name", name));
        if (required) heading.append(" ", element("span", "ai-tool-schema-required text-danger", labels.required));
        const type = schema?.type || (schema?.properties ? "object" : labels.any);
        heading.append(" ", element("span", "ai-tool-schema-type", Array.isArray(type) ? type.join(" | ") : type));
        if (schema?.description) heading.append(" ", element("span", "ai-tool-schema-description", schema.description));
        const constraints = [];
        for (const keyword of ["enum", "const", "default", "minimum", "maximum", "minLength", "maxLength", "pattern", "format"]) {
            if (schema?.[keyword] != null) constraints.push(keyword + ": " + JSON.stringify(schema[keyword]));
        }
        if (schema?.additionalProperties === false) constraints.push("additionalProperties: false");
        if (constraints.length) heading.append(element("code", "ai-tool-schema-constraints", constraints.join(" · ")));
        node.append(heading);
        if (fields.length) {
            const content = element("div", "ai-tool-schema-children");
            node.append(content);
            let populated = false;
            const populate = () => {
                if (populated || !node.open) return;
                populated = true;
                for (const child of fields) content.append(field(child, depth + 1));
            };
            node.addEventListener("toggle", populate);
            // Deeper models, including recursive references, are built only when opened.
            node.open = depth < 2;
            populate();
        }
        return node;
    }

    container.replaceChildren(field({ name: "", schema: root, path: "" }, 0));
    container.scrollTop = 0;
}
