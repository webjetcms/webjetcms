package sk.iway.iwcm.branch;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Versioned canonical supported content, separate from identity and runtime data. */
public record BranchSnapshot(State state, Map<String, Object> fields) {
    public enum State { ACTIVE, TRASH, ABSENT }
    private static final ObjectMapper JSON = new ObjectMapper();

    public BranchSnapshot {
        Objects.requireNonNull(state);
        Objects.requireNonNull(fields);
        if (state == State.ABSENT && !fields.isEmpty()) throw new IllegalArgumentException("ABSENT has no content");
        fields = canonicalMap(fields);
    }

    public static BranchSnapshot absent() { return new BranchSnapshot(State.ABSENT, Map.of()); }

    /** A directory marker has existence and kind, but no byte hash or binary metadata. */
    public static BranchSnapshot file(State state, String logicalPath, String entryKind, String contentHash) {
        if (state == State.ABSENT) return absent();
        if (!"FILE".equals(entryKind) && !"DIRECTORY".equals(entryKind)) throw new IllegalArgumentException("Invalid file entry kind");
        if (logicalPath == null || !logicalPath.startsWith("/")) throw new IllegalArgumentException("Missing logical path");
        if ("DIRECTORY".equals(entryKind) && contentHash != null) throw new IllegalArgumentException("A directory has no byte hash");
        if ("FILE".equals(entryKind) && (contentHash == null || !contentHash.matches("[0-9a-f]{64}"))) throw new IllegalArgumentException("Missing file content hash");
        Map<String, Object> values = new TreeMap<>();
        values.put("logical_path", logicalPath);
        values.put("entry_kind", entryKind);
        values.put("content_hash", contentHash);
        return new BranchSnapshot(state, values);
    }

    public String json() {
        try {
            Map<String, Object> envelope = new TreeMap<>();
            envelope.put("version", 1);
            envelope.put("state", state.name());
            envelope.put("fields", fields);
            return JSON.writeValueAsString(envelope);
        } catch (Exception e) { throw new IllegalStateException("Cannot serialize branch snapshot", e); }
    }

    public String hash() {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json().getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    public static BranchSnapshot fromJson(String json) {
        try {
            Map<String, Object> envelope = JSON.readValue(json, new TypeReference<Map<String, Object>>() { });
            if (!(envelope.get("version") instanceof Number version) || version.intValue() != 1) throw new IllegalArgumentException("Unsupported snapshot version");
            Object rawFields = envelope.get("fields");
            if (!(rawFields instanceof Map<?, ?> raw)) throw new IllegalArgumentException("Missing snapshot content");
            Map<String, Object> values = new TreeMap<>();
            raw.forEach((key, value) -> values.put((String) key, value));
            return new BranchSnapshot(State.valueOf((String) envelope.get("state")), values);
        } catch (Exception e) { throw new IllegalArgumentException("Invalid branch snapshot", e); }
    }

    private static Map<String, Object> canonicalMap(Map<String, ?> source) {
        Map<String, Object> target = new TreeMap<>();
        source.forEach((name, value) -> target.put(name, canonical(value)));
        return Collections.unmodifiableMap(target);
    }

    private static Object canonical(Object value) {
        if (value == null || value instanceof String || value instanceof Boolean) return value;
        if (value instanceof java.util.Date date) return canonical(date.getTime());
        if (value instanceof java.time.Instant instant) return canonical(instant.toEpochMilli());
        if (value instanceof Number number) {
            BigDecimal decimal = new BigDecimal(number.toString()).stripTrailingZeros();
            return decimal.signum() == 0 ? BigDecimal.ZERO : decimal;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> converted = new TreeMap<>();
            map.forEach((key, item) -> converted.put((String) key, canonical(item)));
            return Collections.unmodifiableMap(converted);
        }
        if (value instanceof Iterable<?> items) {
            List<Object> converted = new ArrayList<>();
            items.forEach(item -> converted.add(canonical(item)));
            return Collections.unmodifiableList(converted);
        }
        throw new IllegalArgumentException("Unsupported snapshot value: " + value.getClass().getName());
    }
}
