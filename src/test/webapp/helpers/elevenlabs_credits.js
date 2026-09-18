const { getSafeErrorDetail, readApiErrorDetail } = require("./elevenlabs_client.js");

/** Reads a bounded billing snapshot without allowing reporting errors to stop generation. */
async function readSubscription({ apiKey, fetchImpl = globalThis.fetch, timeoutMs = 10_000 }) {
  try {
    const response = await fetchImpl("https://api.elevenlabs.io/v1/user/subscription", {
      headers: { "xi-api-key": apiKey, Accept: "application/json" },
      redirect: "error",
      signal: AbortSignal.timeout(timeoutMs)
    });
    if (!response.ok) throw new Error(`HTTP ${response.status}: ${await readApiErrorDetail(response)}`);
    const data = await response.json();
    if (![data.character_count, data.character_limit].every(value => Number.isFinite(value) && value >= 0)) {
      throw new Error("Subscription response is missing valid credit counters.");
    }
    return { used: data.character_count, limit: data.character_limit,
      reset: Number.isFinite(data.next_character_count_reset_unix) && data.next_character_count_reset_unix > 0
        ? data.next_character_count_reset_unix : null };
  } catch (error) {
    return { error: getSafeErrorDetail(error.message).replaceAll(apiKey, "[redacted]") || "Subscription unavailable" };
  }
}

/** Formats exact TTS charges or an explicitly approximate workspace usage difference. */
function formatCreditSummary({ label, before, after, cost = null, provisional = false }) {
  let reason = before.error || after.error;
  if (!reason && (before.reset == null || after.reset == null)) reason = "Billing period information unavailable";
  if (!reason && (before.reset !== after.reset || after.used < before.used)) reason = "Billing period or credit counter changed";
  const usage = cost != null ? `${cost} (TTS response)` : reason
    ? `unavailable (${reason})`
    : `${after.used - before.used} (approximate account delta; concurrent or delayed charges may affect it)`;
  const remaining = after.error ? `unavailable (${after.error})` : Math.max(0, after.limit - after.used);
  return `[ElevenLabs ${label}] Credits used${provisional ? " so far (generation still pending or uncertain)" : ""}: ${usage} | Credits remaining in current limit: ${remaining}`;
}

/** Starts one per-run report; finish is non-throwing and never masks a generation failure. */
async function startCreditReport({ apiKey, label, exactCost = false, fetchImpl = globalThis.fetch, log = console.log }) {
  const before = await readSubscription({ apiKey, fetchImpl });
  let cost = null;
  return {
    recordCost(value) { cost = (cost ?? 0) + value; },
    async finish({ provisional = false } = {}) {
      try {
        const after = await readSubscription({ apiKey, fetchImpl });
        log(formatCreditSummary({ label, before, after, cost: exactCost ? cost : null, provisional }));
      } catch {
        // Reporting must not replace a successful artifact or the original generation error.
      }
    }
  };
}

module.exports = { readSubscription, formatCreditSummary, startCreditReport };
