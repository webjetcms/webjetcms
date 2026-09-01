const DEFAULT_VIDEO_WIDTH = 1920;
const DEFAULT_VIDEO_HEIGHT = 1080;
const DEFAULT_VIDEO_ZOOM = 1;

function getDimension(value, fallback) {
  const dimension = Number.parseInt(value || String(fallback), 10);
  return Number.isNaN(dimension) || dimension < 1 ? fallback : dimension;
}

function getZoom(value) {
  if (value == null || value.trim() === "") return DEFAULT_VIDEO_ZOOM;

  const normalizedValue = value.trim();
  const zoom = normalizedValue.endsWith("%")
    ? Number(normalizedValue.slice(0, -1)) / 100
    : Number(normalizedValue);
  if (!Number.isFinite(zoom) || zoom <= 0) {
    throw new Error("Video zoom must be a finite number greater than zero or a positive percentage.");
  }
  return zoom;
}

function getVideoSettings(environment = process.env) {
  const width = getDimension(environment.CODECEPT_VIDEO_WIDTH, DEFAULT_VIDEO_WIDTH);
  const height = getDimension(environment.CODECEPT_VIDEO_HEIGHT, DEFAULT_VIDEO_HEIGHT);
  const zoom = getZoom(environment.CODECEPT_VIDEO_ZOOM);

  return {
    width,
    height,
    zoom,
    viewportWidth: Math.max(1, Math.round(width / zoom)),
    viewportHeight: Math.max(1, Math.round(height / zoom))
  };
}

module.exports = { getVideoSettings };
