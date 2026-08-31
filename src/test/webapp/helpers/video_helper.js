const { Helper } = codeceptjs;

const DEFAULT_CLICK_DELAY = 350;
const DEFAULT_POST_CLICK_DELAY = 500;
const CURSOR_MIN_DURATION = 520;
const CURSOR_MAX_DURATION = 900;
const CURSOR_FRAME_DURATION = 16;

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

function clamp(value, minimum, maximum) {
  return Math.min(Math.max(value, minimum), maximum);
}

function easeInOutCubic(progress) {
  if (progress < 0.5) return 4 * progress * progress * progress;
  return 1 - Math.pow(-2 * progress + 2, 3) / 2;
}

function cubicBezierPoint(start, firstControl, secondControl, end, progress) {
  const inverse = 1 - progress;
  const startWeight = inverse * inverse * inverse;
  const firstWeight = 3 * inverse * inverse * progress;
  const secondWeight = 3 * inverse * progress * progress;
  const endWeight = progress * progress * progress;
  return {
    x: startWeight * start.x + firstWeight * firstControl.x + secondWeight * secondControl.x + endWeight * end.x,
    y: startWeight * start.y + firstWeight * firstControl.y + secondWeight * secondControl.y + endWeight * end.y
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
      border: "3px solid #00a88f",
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
  async _before() {
    if (!isCursorEnabled()) return;

    const { browserContext, page } = this.helpers.Playwright;
    this.videoCursorPosition = null;
    this.videoCursorPage = null;
    this.videoCurveDirection = 1;
    await browserContext.addInitScript(installVideoCursor);
    await page.evaluate(installVideoCursor);
  }

  async _moveCursorAlongCurve(locator) {
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
    const start = {
      x: clamp(rawStart.x, 0, viewport.width),
      y: clamp(rawStart.y, 0, viewport.height)
    };
    const end = {
      x: box.x + box.width / 2,
      y: box.y + box.height / 2
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

    const direction = this.videoCurveDirection || 1;
    this.videoCurveDirection = -direction;
    const normalX = -deltaY / distance;
    const normalY = deltaX / distance;
    const amplitude = Math.min(distance * 0.14, 120) * direction;
    const firstControl = {
      x: clamp(start.x + deltaX * 0.28 + normalX * amplitude, 0, viewport.width),
      y: clamp(start.y + deltaY * 0.28 + normalY * amplitude, 0, viewport.height)
    };
    const secondControl = {
      x: clamp(start.x + deltaX * 0.72 - normalX * amplitude, 0, viewport.width),
      y: clamp(start.y + deltaY * 0.72 - normalY * amplitude, 0, viewport.height)
    };
    const duration = clamp(480 + distance * 0.25, CURSOR_MIN_DURATION, CURSOR_MAX_DURATION);
    const steps = Math.max(24, Math.ceil(duration / CURSOR_FRAME_DURATION));
    const frameDuration = duration / steps;

    await page.mouse.move(start.x, start.y);
    for (let step = 1; step <= steps; step++) {
      const progress = easeInOutCubic(step / steps);
      const point = cubicBezierPoint(start, firstControl, secondControl, end, progress);
      await page.mouse.move(point.x, point.y);
      if (step < steps) {
        await new Promise((resolve) => setTimeout(resolve, frameDuration));
      }
    }

    this.videoCursorPosition = end;
    this.videoCursorPage = page;
  }

  /**
   * Moves the synthetic cursor to a target, clicks it, and leaves editing room after the click.
   * @param {*} locator CodeceptJS locator of the target element
   */
  async videoClick(locator) {
    const helper = this.helpers.Playwright;
    if (isCursorEnabled()) {
      await this._moveCursorAlongCurve(locator);
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
