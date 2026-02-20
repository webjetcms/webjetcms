package sk.iway.iwcm.system.stripes;

import net.sourceforge.stripes.config.Configuration;
import net.sourceforge.stripes.format.DefaultFormatterFactory;

import java.util.Date;

/**
 * Custom DefaultFormatterFactory for Stripes framework using our custom Date formatter
 */
public class StripesFormatterFactory extends DefaultFormatterFactory {

    @Override
    public void init(Configuration configuration) throws Exception {
        super.init(configuration);

        add(Date.class, StripesDateFormatter.class);
    }

}