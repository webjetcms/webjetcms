const { Helper } = codeceptjs;

const DEFAULT_CLICK_DELAY = 350;

function isCursorEnabled() {
  return "true" === process.env.CODECEPT_VIDEO_CURSOR;
}

function getClickDelay() {
  const value = Number.parseInt(process.env.CODECEPT_VIDEO_CLICK_DELAY || DEFAULT_CLICK_DELAY, 10);
  if (Number.isNaN(value) || value < 0) return DEFAULT_CLICK_DELAY;
  return Math.min(value, 2000);
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
      transition: "transform 220ms cubic-bezier(.22, .8, .25, 1), opacity 100ms linear",
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
    await browserContext.addInitScript(installVideoCursor);
    await page.evaluate(installVideoCursor);
  }

  /**
   * Moves the synthetic cursor to a target and clicks it after a short presentation delay.
   * @param {*} locator CodeceptJS locator of the target element
   */
  async videoClick(locator) {
    const helper = this.helpers.Playwright;
    await helper.moveCursorTo(locator);
    if (isCursorEnabled()) {
      await new Promise((resolve) => setTimeout(resolve, getClickDelay()));
    }
    return helper.click(locator);
  }
}

module.exports = VideoHelper;
