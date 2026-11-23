package sk.iway.iwcm.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.springframework.mock.web.MockHttpSession;

/**
 * Serializes attributes on writes and returns detached copies on reads.
 * Exposes missing session updates without requiring a Redis server or a particular codec.
 */
public class SerializingMockHttpSession extends MockHttpSession {
    @Override
    public void setAttribute(String name, Object value) {
        if (value == null) {
            super.setAttribute(name, null);
            return;
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Cannot serialize session attribute " + name, exception);
        }
        super.setAttribute(name, bytes.toByteArray());
    }

    @Override
    public Object getAttribute(String name) {
        byte[] bytes = (byte[]) super.getAttribute(name);
        if (bytes == null) return null;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return in.readObject();
        } catch (IOException | ClassNotFoundException exception) {
            throw new IllegalArgumentException("Cannot deserialize session attribute " + name, exception);
        }
    }
}
