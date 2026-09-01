const fs = require("node:fs");
const path = require("node:path");
const { spawnSync } = require("node:child_process");
const acorn = require("acorn");

const WEBAPP_ROOT = path.resolve(__dirname, "..");
const CODECEPT_BIN = require.resolve("codeceptjs/bin/codecept.js");
const AUDIO_SCENARIO_TITLE = "ElevenLabs";
const AUDIO_TAG = "@audio";
const AUDIO_GREP = "(^|\\s)@audio(\\s|$)";
const CODECEPT_HOOK_NAMES = new Set([
  "After",
  "AfterStep",
  "AfterSuite",
  "Before",
  "BeforeStep",
  "BeforeSuite"
]);

function isPathInside(parentPath, candidatePath) {
  const relativePath = path.relative(parentPath, candidatePath);
  return relativePath !== "" &&
    relativePath !== ".." &&
    !relativePath.startsWith(`..${path.sep}`) &&
    !path.isAbsolute(relativePath);
}

function getStringLiteral(node) {
  return node?.type === "Literal" && typeof node.value === "string" ? node.value : null;
}

function unwrapDefinitionExpression(expression, definitionName) {
  const modifiers = [];
  let current = expression;

  while (current?.type === "CallExpression") {
    if (current.callee?.type === "Identifier" && current.callee.name === definitionName) {
      return { call: current, modifiers };
    }

    const callee = current.callee;
    if (callee?.type !== "MemberExpression" || callee.computed ||
      callee.property?.type !== "Identifier") {
      return null;
    }
    modifiers.push({
      arguments: current.arguments,
      name: callee.property.name
    });
    current = callee.object;
  }
  return null;
}

function isAudioTag(modifier) {
  return modifier.name === "tag" && getStringLiteral(modifier.arguments[0]) === AUDIO_TAG;
}

function matchesAudioGrep(value) {
  return typeof value === "string" && new RegExp(AUDIO_GREP).test(value);
}

function tagMatchesAudioGrep(modifier) {
  const tag = modifier.name === "tag" ? getStringLiteral(modifier.arguments[0]) : null;
  return matchesAudioGrep(tag);
}

function containsEagerCall(node) {
  if (node == null || typeof node !== "object") return false;
  if (["CallExpression", "NewExpression", "TaggedTemplateExpression"].includes(node.type)) {
    return true;
  }
  if (["ArrowFunctionExpression", "FunctionExpression"].includes(node.type)) {
    return false;
  }
  return Object.values(node).some((value) => {
    if (Array.isArray(value)) return value.some(containsEagerCall);
    return value?.type != null && containsEagerCall(value);
  });
}

function validateGenerateAudioOptions(node, fail) {
  if (node.type !== "ObjectExpression") {
    fail("I.generateAudio options must be an object literal.");
  }

  const allowedNames = new Set(["modelId", "voiceId"]);
  const configuredNames = new Set();
  for (const property of node.properties) {
    const propertyName = property.key?.type === "Identifier"
      ? property.key.name
      : getStringLiteral(property.key);
    if (property.type !== "Property" || property.kind !== "init" || property.computed ||
      property.method || !allowedNames.has(propertyName)) {
      fail("I.generateAudio options may contain only modelId and voiceId string literals.");
    }
    if (configuredNames.has(propertyName)) {
      fail(`I.generateAudio option ${propertyName} must not be repeated.`);
    }
    configuredNames.add(propertyName);
    if (getStringLiteral(property.value)?.trim() === "" || getStringLiteral(property.value) == null) {
      fail(`I.generateAudio option ${propertyName} must be a non-empty string literal.`);
    }
  }
}

function validateAudioScenarioSource(source, sourcePath = "<audio scenario>") {
  const fail = (message) => {
    throw new Error(`Invalid audio scenario ${sourcePath}: ${message}`);
  };

  let syntaxTree;
  try {
    syntaxTree = acorn.parse(source, {
      ecmaVersion: "latest",
      locations: true,
      sourceType: "script"
    });
  } catch (error) {
    fail(`JavaScript parsing failed: ${error.message}`);
  }

  const globalHook = syntaxTree.body.find((statement) => {
    const callee = statement.type === "ExpressionStatement"
      ? statement.expression?.callee
      : null;
    return callee?.type === "Identifier" && CODECEPT_HOOK_NAMES.has(callee.name);
  });
  if (globalHook != null) {
    fail(`${globalHook.expression.callee.name} hooks are not allowed in audio scenario files.`);
  }

  const unsupportedStatement = syntaxTree.body.find((statement) => {
    if (["EmptyStatement", "ExpressionStatement", "FunctionDeclaration"].includes(statement.type)) {
      return false;
    }
    if (statement.type === "VariableDeclaration") {
      return statement.declarations.some((declaration) => containsEagerCall(declaration.init));
    }
    return true;
  });
  if (unsupportedStatement != null) {
    fail("Audio scenario files may contain only static declarations and direct Feature or Scenario calls.");
  }

  const expressionStatements = syntaxTree.body
    .filter((statement) => statement.type === "ExpressionStatement")
    .map((statement) => statement.expression);
  const unsupportedExpression = expressionStatements.find((expression) => {
    const isDirective = expression.type === "Literal" && typeof expression.value === "string";
    return !isDirective &&
      unwrapDefinitionExpression(expression, "Feature") == null &&
      unwrapDefinitionExpression(expression, "Scenario") == null;
  });
  if (unsupportedExpression != null) {
    fail("Only direct Feature and Scenario declarations are allowed at the top level.");
  }
  const features = expressionStatements
    .map((expression) => unwrapDefinitionExpression(expression, "Feature"))
    .filter(Boolean);
  const scenarios = expressionStatements
    .map((expression) => unwrapDefinitionExpression(expression, "Scenario"))
    .filter(Boolean);

  if (features.length !== 1 || getStringLiteral(features[0]?.call.arguments[0]) == null) {
    fail(`expected exactly one Feature with a static title, found ${features.length}.`);
  }
  const featureTitle = getStringLiteral(features[0].call.arguments[0]);
  const dynamicFeatureTag = features[0].modifiers.find(
    (modifier) => modifier.name === "tag" && getStringLiteral(modifier.arguments[0]) == null
  );
  if (dynamicFeatureTag != null) {
    fail("Feature tags must be static string literals in audio scenario files.");
  }
  if (matchesAudioGrep(featureTitle) || features[0].modifiers.some(tagMatchesAudioGrep)) {
    fail(`${AUDIO_TAG} must not appear in the Feature title or tags.`);
  }
  for (const scenario of scenarios) {
    const scenarioTitle = getStringLiteral(scenario.call.arguments[0]);
    if (scenarioTitle == null) {
      fail("Every Scenario must have a static title.");
    }
    if (matchesAudioGrep(scenarioTitle)) {
      fail(`${AUDIO_TAG} must not appear in a Scenario title.`);
    }
    const dynamicTag = scenario.modifiers.find(
      (modifier) => modifier.name === "tag" && getStringLiteral(modifier.arguments[0]) == null
    );
    if (dynamicTag != null) {
      fail("Scenario tags must be static string literals in audio scenario files.");
    }
  }

  const namedScenarios = scenarios.filter(
    (scenario) => getStringLiteral(scenario.call.arguments[0]) === AUDIO_SCENARIO_TITLE
  );
  const taggedScenarios = scenarios.filter(
    (scenario) => scenario.modifiers.some(tagMatchesAudioGrep)
  );

  if (namedScenarios.length !== 1) {
    fail(`expected exactly one Scenario("${AUDIO_SCENARIO_TITLE}"), found ${namedScenarios.length}.`);
  }
  if (taggedScenarios.length !== 1) {
    fail(`expected exactly one scenario tagged ${AUDIO_TAG}, found ${taggedScenarios.length}.`);
  }

  const audioScenario = namedScenarios[0];
  if (audioScenario !== taggedScenarios[0]) {
    fail(`Scenario("${AUDIO_SCENARIO_TITLE}") must be the scenario tagged ${AUDIO_TAG}.`);
  }
  if (audioScenario.modifiers.length !== 1 || !isAudioTag(audioScenario.modifiers[0]) ||
    audioScenario.modifiers[0].arguments.length !== 1) {
    fail(`Scenario("${AUDIO_SCENARIO_TITLE}") must have only the ${AUDIO_TAG} tag.`);
  }
  if (audioScenario.call.arguments.length !== 2) {
    fail(`Scenario("${AUDIO_SCENARIO_TITLE}") must contain a single callback.`);
  }

  const callback = audioScenario.call.arguments[1];
  const injectedParameter = callback?.params?.[0];
  const injectedProperty = injectedParameter?.properties?.[0];
  const injectsOnlyI = callback?.type === "ArrowFunctionExpression" && !callback.async &&
    callback.params.length === 1 && injectedParameter.type === "ObjectPattern" &&
    injectedParameter.properties.length === 1 && injectedProperty.type === "Property" &&
    !injectedProperty.computed && injectedProperty.key?.type === "Identifier" &&
    injectedProperty.key.name === "I" && injectedProperty.value?.type === "Identifier" &&
    injectedProperty.value.name === "I";
  if (!injectsOnlyI || callback.body?.type !== "BlockStatement") {
    fail(`Scenario("${AUDIO_SCENARIO_TITLE}") must inject only I into a synchronous callback.`);
  }
  if (callback.body.body.length !== 1 || callback.body.body[0].type !== "ExpressionStatement") {
    fail(`Scenario("${AUDIO_SCENARIO_TITLE}") must contain only one I.generateAudio call.`);
  }

  const generateAudioCall = callback.body.body[0].expression;
  const callee = generateAudioCall?.callee;
  const isGenerateAudioCall = generateAudioCall?.type === "CallExpression" &&
    callee?.type === "MemberExpression" && !callee.computed &&
    callee.object?.type === "Identifier" && callee.object.name === "I" &&
    callee.property?.type === "Identifier" && callee.property.name === "generateAudio";
  if (!isGenerateAudioCall || generateAudioCall.arguments.length < 1 ||
    generateAudioCall.arguments.length > 2) {
    fail(`Scenario("${AUDIO_SCENARIO_TITLE}") must contain only one I.generateAudio call.`);
  }

  const narration = generateAudioCall.arguments[0];
  const narrationText = narration?.quasis?.[0]?.value?.cooked;
  if (narration?.type !== "TemplateLiteral" || narration.expressions.length !== 0 ||
    narration.quasis.length !== 1 || typeof narrationText !== "string" ||
    narrationText.trim() === "") {
    fail("I.generateAudio narration must be a non-empty template literal without interpolation.");
  }
  if (generateAudioCall.arguments.length === 2) {
    validateGenerateAudioOptions(generateAudioCall.arguments[1], fail);
  }
}

function resolveAudioScenario(argument, options = {}) {
  const webappRoot = options.webappRoot || WEBAPP_ROOT;
  const fsImpl = options.fsImpl || fs;
  const videoDirectory = fsImpl.realpathSync(path.join(webappRoot, "video"));

  if (typeof argument !== "string" || path.extname(argument) !== ".js") {
    throw new Error("The audio scenario must be a .js file.");
  }

  let scenarioPath;
  try {
    scenarioPath = fsImpl.realpathSync(path.resolve(webappRoot, argument));
  } catch (error) {
    if (error.code === "ENOENT") {
      throw new Error(`Audio scenario does not exist: ${argument}`);
    }
    throw error;
  }

  if (!isPathInside(videoDirectory, scenarioPath)) {
    throw new Error("The audio scenario must be located inside the video directory.");
  }
  if (!fsImpl.statSync(scenarioPath).isFile()) {
    throw new Error("The audio scenario must be a file.");
  }
  validateAudioScenarioSource(fsImpl.readFileSync(scenarioPath, "utf8"), scenarioPath);
  return scenarioPath;
}

function runAudio(argv, options = {}) {
  const stderr = options.stderr || ((message) => console.error(message));
  if (argv.length !== 1) {
    stderr("Usage: npm run audio video/<scenario>.js");
    return 1;
  }

  let scenarioPath;
  try {
    scenarioPath = resolveAudioScenario(argv[0], options);
  } catch (error) {
    stderr(error.message);
    return 1;
  }

  const webappRoot = options.webappRoot || WEBAPP_ROOT;
  const codeceptBin = options.codeceptBin || CODECEPT_BIN;
  const spawn = options.spawnSync || spawnSync;
  const result = spawn(process.execPath, [
    codeceptBin,
    "run",
    "-c",
    path.join(webappRoot, "codecept.audio.conf.js"),
    "--steps",
    "--grep",
    AUDIO_GREP
  ], {
    cwd: webappRoot,
    env: {
      ...process.env,
      CODECEPT_AUDIO_FILE: scenarioPath
    },
    stdio: "inherit"
  });

  if (result.error != null) {
    stderr(`Unable to start CodeceptJS: ${result.error.message}`);
    return 1;
  }
  return Number.isInteger(result.status) ? result.status : 1;
}

if (require.main === module) {
  process.exitCode = runAudio(process.argv.slice(2));
}

module.exports = {
  AUDIO_GREP,
  isPathInside,
  resolveAudioScenario,
  runAudio,
  validateAudioScenarioSource
};
