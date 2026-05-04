package sk.iway.iwcm.components.gdpr.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import sk.iway.iwcm.components.gdpr.GdprModule;

public class GdprResults {
    private Map<GdprModule, List<GdprResult>> results;

    public GdprResults() {
        results = new LinkedHashMap<>();
    }

    public void put(GdprModule module, List<GdprResult> results) {
        this.results.put(module, results);
    }

    public List<GdprResult> get(GdprModule module) {
        return this.results.get(module);
    }

    public int size(GdprModule module) {
        return results.containsKey(module) ? results.get(module).size() : 0;
    }

    public Map<GdprModule, List<GdprResult>> getResults() {
        return results;
    }
}
