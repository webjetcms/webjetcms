package sk.iway.iwcm.components.ai.providers;


import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.http.client.methods.HttpRequestBase;

import com.fasterxml.jackson.databind.JsonNode;

import sk.iway.iwcm.system.datatable.json.LabelValue;

public class FunctionTypes {
    public interface ModelsRequest extends Supplier<HttpRequestBase> {}
    public interface ModelsExtractor extends Function<JsonNode, List<LabelValue>> {}

    @FunctionalInterface
    public interface ResponseRequest { HttpRequestBase apply(String a, String b, String c); }

    public interface ResponseExtractor extends Function<JsonNode, String> {}
}
