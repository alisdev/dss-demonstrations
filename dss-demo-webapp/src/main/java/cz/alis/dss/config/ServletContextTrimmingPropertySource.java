package cz.alis.dss.config;

import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.support.ServletContextPropertySource;

/**
 * Zdroj properties načtených ze servlet kontextu. Funguje stejně jako
 * {@link ServletContextPropertySource} jen trimuje hodnoty properties.
 * 
 */
public class ServletContextTrimmingPropertySource extends ServletContextPropertySource {

    public ServletContextTrimmingPropertySource( String name, ServletContext servletContext ) {
        super( name, servletContext );
    }

    @Override
    public String getProperty( String name ) {
        return StringUtils.trim( super.getProperty( name ) );
    }

}
