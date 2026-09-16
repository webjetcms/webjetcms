const fs = require("node:fs");
const path = require("node:path");
const acorn = require("acorn");

function isPathInside(parentPath, candidatePath) {
  const relativePath = path.relative(parentPath, candidatePath);
  return relativePath !== "" && relativePath !== ".." &&
    !relativePath.startsWith(`..${path.sep}`) && !path.isAbsolute(relativePath);
}

/** Resolves one existing JavaScript scenario inside the video directory, including symlink checks. */
function resolveVideoScenarioPath(argument, options = {}) {
  const webappRoot = options.webappRoot || path.resolve(__dirname, "..");
  const fsImpl = options.fsImpl || fs;
  const videoDirectory = fsImpl.realpathSync(path.join(webappRoot, "video"));
  if (typeof argument !== "string" || path.extname(argument) !== ".js") {
    throw new Error("The video scenario must be a .js file.");
  }
  let scenarioPath;
  try {
    scenarioPath = fsImpl.realpathSync(path.resolve(webappRoot, argument));
  } catch (error) {
    if (error.code === "ENOENT") throw new Error(`Video scenario does not exist: ${argument}`);
    throw error;
  }
  if (!isPathInside(videoDirectory, scenarioPath)) {
    throw new Error("The video scenario must be located inside the video directory.");
  }
  if (!fsImpl.statSync(scenarioPath).isFile()) throw new Error("The video scenario must be a file.");
  return scenarioPath;
}

/** Reads literal plan metadata from the AST, skipping inline callbacks without evaluating code. */
function readPlanMetadata(node, location = []) {
  const label = location.join(".") || "plan";
  if (node?.type === "Literal" && (node.value === null ||
    ["string", "boolean", "number"].includes(typeof node.value))) return node.value;
  if (node?.type === "TemplateLiteral" && node.expressions.length === 0) return node.quasis[0].value.cooked;
  if (node?.type === "ArrayExpression") {
    return node.elements.map((element, index) => readPlanMetadata(element, [...location, index]));
  }
  if (node?.type === "ObjectExpression") {
    const result = Object.create(null);
    for (const property of node.properties) {
      const name = property.key?.type === "Identifier" ? property.key.name
        : property.key?.type === "Literal" && typeof property.key.value === "string" ? property.key.value : null;
      if (property.type !== "Property" || property.kind !== "init" || property.computed ||
        property.shorthand || name == null || name === "__proto__") {
        throw new Error(`${label} must use explicit, non-computed data properties.`);
      }
      if (Object.hasOwn(result, name)) throw new Error(`${label}.${name} must not be repeated.`);
      const inlineCallback = location.length === 2 && location[0] === "shots" &&
        typeof location[1] === "number" && ["shot", "prepare"].includes(name);
      if (inlineCallback) {
        if (!["ArrowFunctionExpression", "FunctionExpression"].includes(property.value.type)) {
          throw new Error(`${label}.${name} must be an inline function.`);
        }
        // Keep the key for duplicate detection; narration does not use these callbacks.
        result[name] = undefined;
      } else {
        result[name] = readPlanMetadata(property.value, [...location, name]);
      }
    }
    return result;
  }
  throw new Error(`${label} must contain static literal data; only shot and prepare may be functions.`);
}

/**
 * Reads the canonical top-level videoPlan declaration without importing or executing the scenario.
 * @param {string} source JavaScript scenario source
 * @param {string} [sourcePath] File name used in validation errors
 * @returns {object} Static video plan metadata, with inline callbacks omitted
 */
function readVideoPlanSource(source, sourcePath = "<video scenario>") {
  try {
    const ast = acorn.parse(source, { ecmaVersion: "latest", sourceType: "script" });
    const declarations = ast.body.filter(node => node.type === "VariableDeclaration" && node.kind === "const")
      .flatMap(node => node.declarations).filter(node => node.id.type === "Identifier" && node.id.name === "videoPlan");
    if (declarations.length !== 1 || declarations[0].init?.type !== "ObjectExpression") {
      throw new Error("Expected a top-level const videoPlan object with static shot metadata.");
    }
    return readPlanMetadata(declarations[0].init);
  } catch (error) {
    throw new Error(`Invalid video plan ${sourcePath}: ${error.message}`, { cause: error });
  }
}

module.exports = { isPathInside, resolveVideoScenarioPath, readPlanMetadata, readVideoPlanSource };
