package sk.iway.iwcm.system.spring.webjet_component.dialect;

import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;

import java.util.HashSet;
import java.util.Set;

/**
 * Definicia iwcm tagov, spracovane su na zaklade data-iwcm-MENO atributu
 */
public class IwcmDialect extends AbstractProcessorDialect {

    public IwcmDialect() {
        super("Iwcm Dialect", "iwcm", 1000);
    }

    public Set<IProcessor> getProcessors(final String dialectPrefix) {
        final Set<IProcessor> processors = new HashSet<>();
        processors.add(new WriteAttributeTagProcessor(dialectPrefix));
        processors.add(new CombineAttributeModelProcessor(dialectPrefix));
        processors.add(new ScriptAttributeTagProcessor(dialectPrefix));
        return processors;
    }
}
