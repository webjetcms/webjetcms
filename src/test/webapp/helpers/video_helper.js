const { Helper } = codeceptjs;

const DEFAULT_CLICK_DELAY = 350;
const DEFAULT_POST_CLICK_DELAY = 500;
const DEFAULT_CURVE_STRENGTH = 1;
const CURSOR_MIN_DURATION = 240;
const CURSOR_MAX_DURATION = 900;
const CURSOR_FRAME_DURATION = 25;

function isCursorEnabled() {
  return "true" === process.env.CODECEPT_VIDEO_CURSOR;
}

function getClickDelay() {
  const value = Number.parseInt(process.env.CODECEPT_VIDEO_CLICK_DELAY || DEFAULT_CLICK_DELAY, 10);
  if (Number.isNaN(value) || value < 0) return DEFAULT_CLICK_DELAY;
  return Math.min(value, 2000);
}

function getPostClickDelay() {
  const value = Number.parseInt(process.env.CODECEPT_VIDEO_POST_CLICK_DELAY || DEFAULT_POST_CLICK_DELAY, 10);
  if (Number.isNaN(value) || value < 0) return DEFAULT_POST_CLICK_DELAY;
  return Math.min(Math.max(value, DEFAULT_POST_CLICK_DELAY), 2000);
}

function getCurveStrength(value) {
  if (typeof value !== "number" || !Number.isFinite(value) || value < 0) {
    throw new Error("Video cursor curve strength must be a finite number greater than or equal to zero.");
  }
  return value;
}

function getDefaultCurveStrength() {
  const configuredValue = process.env.CODECEPT_VIDEO_CURVE_STRENGTH;
  if (configuredValue == null || configuredValue.trim() === "") return DEFAULT_CURVE_STRENGTH;
  return getCurveStrength(Number(configuredValue));
}

function clamp(value, minimum, maximum) {
  return Math.min(Math.max(value, minimum), maximum);
}

function hashString(value) {
  let hash = 2166136261;
  for (let index = 0; index < value.length; index++) {
    hash ^= value.charCodeAt(index);
    hash = Math.imul(hash, 16777619);
  }
  return hash >>> 0;
}

function createPseudoRandom(seed) {
  let state = seed >>> 0;
  return () => {
    state = (state + 0x6D2B79F5) >>> 0;
    let value = state;
    value = Math.imul(value ^ (value >>> 15), value | 1);
    value ^= value + Math.imul(value ^ (value >>> 7), value | 61);
    return ((value ^ (value >>> 14)) >>> 0) / 4294967296;
  };
}

function randomBetween(random, minimum, maximum) {
  return minimum + random() * (maximum - minimum);
}

function minimumJerk(progress) {
  return progress * progress * progress * (10 + progress * (-15 + 6 * progress));
}

function getCursorDuration(distance, random) {
  const duration = (230 + Math.sqrt(distance) * 18) * randomBetween(random, 0.92, 1.08);
  return clamp(duration, CURSOR_MIN_DURATION, CURSOR_MAX_DURATION);
}

function normalizedBump(progress, peak, concentration) {
  if (progress <= 0 || progress >= 1) return 0;
  const startExponent = peak * concentration;
  const endExponent = (1 - peak) * concentration;
  const value = Math.pow(progress, startExponent) * Math.pow(1 - progress, endExponent);
  const maximum = Math.pow(peak, startExponent) * Math.pow(1 - peak, endExponent);
  return value / maximum;
}

function getNormalClearance(point, normal, direction, viewport, padding = 36) {
  const distances = [];
  const directionX = normal.x * direction;
  const directionY = normal.y * direction;
  const maximumX = Math.max(padding, viewport.width - padding);
  const maximumY = Math.max(padding, viewport.height - padding);

  if (directionX > 0) distances.push((maximumX - point.x) / directionX);
  if (directionX < 0) distances.push((point.x - padding) / -directionX);
  if (directionY > 0) distances.push((maximumY - point.y) / directionY);
  if (directionY < 0) distances.push((point.y - padding) / -directionY);
  return Math.max(0, Math.min(...distances));
}

function createNaturalCursorPlan(start, end, viewport, distance, random, curveStrength) {
  const delta = { x: end.x - start.x, y: end.y - start.y };
  const normal = { x: -delta.y / distance, y: delta.x / distance };
  const primaryPeak = randomBetween(random, 0.22, 0.4);
  const primaryBase = {
    x: start.x + delta.x * primaryPeak,
    y: start.y + delta.y * primaryPeak
  };
  const positiveClearance = getNormalClearance(primaryBase, normal, 1, viewport);
  const negativeClearance = getNormalClearance(primaryBase, normal, -1, viewport);
  const clearanceTotal = positiveClearance + negativeClearance;
  const primaryDirection = clearanceTotal > 0 &&
    random() * clearanceTotal < positiveClearance ? 1 : -1;
  const primaryRatio = distance < 35
    ? randomBetween(random, 0.03, 0.08)
    : randomBetween(random, 0.08, 0.2);
  const maximumPrimaryAmplitude = getNormalClearance(
    primaryBase,
    normal,
    primaryDirection,
    viewport
  ) * 0.72;
  const defaultPrimaryAmplitude = Math.min(
    distance * primaryRatio,
    120,
    maximumPrimaryAmplitude
  );
  const primaryAmplitude = primaryDirection * Math.min(
    defaultPrimaryAmplitude * curveStrength,
    maximumPrimaryAmplitude
  );

  const secondaryPeak = randomBetween(random, 0.62, 0.78);
  const secondaryBase = {
    x: start.x + delta.x * secondaryPeak,
    y: start.y + delta.y * secondaryPeak
  };
  const secondaryDirection = random() < 0.68 ? -primaryDirection : primaryDirection;
  const secondaryAmplitude = secondaryDirection * Math.min(
    Math.abs(primaryAmplitude) * randomBetween(random, 0.025, 0.08),
    getNormalClearance(secondaryBase, normal, secondaryDirection, viewport) * 0.65
  );

  return {
    delta,
    normal,
    primaryPeak,
    primaryAmplitude,
    primaryConcentration: randomBetween(random, 5, 7.5),
    secondaryPeak,
    secondaryAmplitude,
    secondaryConcentration: randomBetween(random, 5, 7.5)
  };
}

function naturalCursorPoint(start, end, plan, progress) {
  if (progress >= 1) return end;
  const primaryOffset = plan.primaryAmplitude * normalizedBump(
    progress,
    plan.primaryPeak,
    plan.primaryConcentration
  );
  const secondaryOffset = plan.secondaryAmplitude * normalizedBump(
    progress,
    plan.secondaryPeak,
    plan.secondaryConcentration
  );
  const normalOffset = primaryOffset + secondaryOffset;
  return {
    x: start.x + plan.delta.x * progress + plan.normal.x * normalOffset,
    y: start.y + plan.delta.y * progress + plan.normal.y * normalOffset
  };
}

async function getViewportSize(page) {
  const viewport = page.viewportSize();
  if (viewport != null) return viewport;
  return page.evaluate(() => ({ width: window.innerWidth, height: window.innerHeight }));
}

async function getRenderedCursorPosition(page) {
  return page.evaluate(() => {
    const host = document.querySelector("#wj-video-cursor-host");
    const x = Number(host?.dataset.cursorX);
    const y = Number(host?.dataset.cursorY);
    if (!Number.isFinite(x) || !Number.isFinite(y)) return null;
    return { x, y };
  });
}

function installVideoCursor() {
  if (document.querySelector("#wj-video-cursor-host") != null) return;

  const mount = () => {
    if (document.querySelector("#wj-video-cursor-host") != null) return;
    if (document.documentElement == null) return;

    const host = document.createElement("div");
    host.id = "wj-video-cursor-host";
    host.setAttribute("aria-hidden", "true");
    Object.assign(host.style, {
      position: "fixed",
      left: "0",
      top: "0",
      width: "0",
      height: "0",
      overflow: "visible",
      pointerEvents: "none",
      zIndex: "2147483647"
    });

    const shadow = host.attachShadow({ mode: "open" });
    const cursor = document.createElement("div");
    Object.assign(cursor.style, {
      position: "absolute",
      left: "0",
      top: "0",
      width: "30px",
      height: "34px",
      opacity: "0",
      transform: "translate3d(-60px, -60px, 0)",
      transition: "opacity 100ms linear",
      willChange: "transform"
    });

    const ring = document.createElement("span");
    Object.assign(ring.style, {
      position: "absolute",
      left: "-11px",
      top: "-11px",
      width: "26px",
      height: "26px",
      border: "3px solid #00BE9F",
      borderRadius: "50%",
      boxSizing: "border-box",
      opacity: "0"
    });

    const pointer = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    pointer.setAttribute("width", "30");
    pointer.setAttribute("height", "34");
    pointer.setAttribute("viewBox", "0 0 30 34");
    pointer.style.filter = "drop-shadow(0 1px 2px rgba(0, 0, 0, .55))";

    const pointerPath = document.createElementNS("http://www.w3.org/2000/svg", "path");
    pointerPath.setAttribute("d", "M2 2 L2 25 L8.7 18.5 L13.8 30.5 L18.2 28.6 L13.1 16.8 L22.5 16.8 Z");
    pointerPath.setAttribute("fill", "#ffffff");
    pointerPath.setAttribute("stroke", "#111827");
    pointerPath.setAttribute("stroke-width", "2");
    pointerPath.setAttribute("stroke-linejoin", "round");
    pointer.appendChild(pointerPath);

    cursor.appendChild(ring);
    cursor.appendChild(pointer);
    shadow.appendChild(cursor);
    document.documentElement.appendChild(host);

    const updatePosition = (event) => {
      host.dataset.cursorX = String(event.clientX);
      host.dataset.cursorY = String(event.clientY);
      cursor.style.opacity = "1";
      cursor.style.transform = `translate3d(${event.clientX}px, ${event.clientY}px, 0)`;
    };

    document.addEventListener("mousemove", updatePosition, true);
    document.addEventListener("mousedown", (event) => {
      updatePosition(event);
      ring.getAnimations().forEach((animation) => animation.cancel());
      ring.animate([
        { opacity: .95, transform: "scale(.35)" },
        { opacity: 0, transform: "scale(1.8)" }
      ], {
        duration: 450,
        easing: "ease-out"
      });
    }, true);
    window.addEventListener("blur", () => {
      cursor.style.opacity = "0";
    });
  };

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", mount, { once: true });
  } else {
    mount();
  }
}

class VideoHelper extends Helper {

  /**
   * Installs a synthetic cursor in every document created by the current browser context.
   * The cursor is enabled only when CODECEPT_VIDEO_CURSOR is set to true.
   */
  async _before(test) {
    if (!isCursorEnabled()) return;

    const { browserContext, page } = this.helpers.Playwright;
    const scenarioName = String(test?.title || "webjet-video");
    const configuredSeed = process.env.CODECEPT_VIDEO_CURSOR_SEED || "default";
    this.videoCursorPosition = null;
    this.videoCursorPage = null;
    this.videoCursorRandom = createPseudoRandom(hashString(`${scenarioName}:${configuredSeed}`));
    await browserContext.addInitScript(installVideoCursor);
    await page.evaluate(installVideoCursor);
  }

  async _moveCursorNaturally(locator, curveStrength) {
    const helper = this.helpers.Playwright;
    const page = helper.page;
    const element = await helper._locateElement(locator);
    if (element == null) throw new Error(`Unable to locate video cursor target: ${String(locator)}`);

    await element.scrollIntoViewIfNeeded();
    const box = await element.boundingBox();
    if (box == null) throw new Error(`Video cursor target is not visible: ${String(locator)}`);

    const viewport = await getViewportSize(page);
    const renderedPosition = await getRenderedCursorPosition(page);
    const rememberedPosition = this.videoCursorPage === page ? this.videoCursorPosition : null;
    const fallbackPosition = { x: viewport.width / 2, y: viewport.height / 2 };
    // The remembered position uses main-viewport coordinates even when the last target was in an iframe.
    const rawStart = rememberedPosition || renderedPosition || fallbackPosition;
    const maximumX = Math.max(0, viewport.width - 1);
    const maximumY = Math.max(0, viewport.height - 1);
    const start = {
      x: clamp(rawStart.x, 0, maximumX),
      y: clamp(rawStart.y, 0, maximumY)
    };
    const end = {
      x: clamp(box.x + box.width / 2, 0, maximumX),
      y: clamp(box.y + box.height / 2, 0, maximumY)
    };

    const deltaX = end.x - start.x;
    const deltaY = end.y - start.y;
    const distance = Math.hypot(deltaX, deltaY);
    if (distance < 1) {
      await page.mouse.move(end.x, end.y);
      this.videoCursorPosition = end;
      this.videoCursorPage = page;
      return;
    }

    const random = this.videoCursorRandom || Math.random;
    const plan = createNaturalCursorPlan(start, end, viewport, distance, random, curveStrength);
    const duration = getCursorDuration(distance, random);
    const steps = Math.max(12, Math.ceil(duration / CURSOR_FRAME_DURATION));
    const frameDuration = duration / steps;

    await page.mouse.move(start.x, start.y);
    const movementStartedAt = Date.now();
    for (let step = 1; step <= steps; step++) {
      const progress = minimumJerk(step / steps);
      const point = naturalCursorPoint(start, end, plan, progress);
      await page.mouse.move(
        clamp(point.x, 0, maximumX),
        clamp(point.y, 0, maximumY)
      );
      if (step < steps) {
        const nextFrameAt = movementStartedAt + step * frameDuration;
        const remainingFrameTime = Math.max(0, nextFrameAt - Date.now());
        if (remainingFrameTime > 0) {
          await new Promise((resolve) => setTimeout(resolve, remainingFrameTime));
        }
      }
    }

    this.videoCursorPosition = end;
    this.videoCursorPage = page;
  }

  /**
   * Moves the synthetic cursor to a target, clicks it, and leaves editing room after the click.
   * @param {*} locator CodeceptJS locator of the target element
   * @param {number} [curveStrength] Finite non-negative curve multiplier; when omitted, the
   * environment default is used, zero produces a straight path, and one uses the baseline curve
   * @throws {Error} When curveStrength is negative, non-finite, or not a number
   */
  async videoClick(locator, curveStrength = getDefaultCurveStrength()) {
    const helper = this.helpers.Playwright;
    const resolvedCurveStrength = getCurveStrength(curveStrength);
    if (isCursorEnabled()) {
      await this._moveCursorNaturally(locator, resolvedCurveStrength);
      await new Promise((resolve) => setTimeout(resolve, getClickDelay()));
    } else {
      await helper.moveCursorTo(locator);
    }
    const result = await helper.click(locator);
    await new Promise((resolve) => setTimeout(resolve, getPostClickDelay()));
    return result;
  }
}

module.exports = VideoHelper;
