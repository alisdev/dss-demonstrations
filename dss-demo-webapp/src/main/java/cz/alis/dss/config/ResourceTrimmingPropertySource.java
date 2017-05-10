package cz.alis.dss.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Subclass of {@link PropertiesPropertySource} that loads a {@link Properties} object from a given
 * {@link org.springframework.core.io.Resource} or resource location such as
 * {@code "classpath:/com/myco/foo.properties"} or {@code "file:/path/to/file.properties"}.
 * 
 * @author Chris Beams
 * @since 3.1
 */
public class ResourceTrimmingPropertySource extends PropertiesPropertySource {

    /**
     * Create a PropertySource having the given name based on Properties loaded from the given
     * resource.
     */
    public ResourceTrimmingPropertySource( String name, Resource resource ) throws IOException {
        super( name, loadPropertiesForResource( resource ) );
    }

    /**
     * Create a PropertySource based on Properties loaded from the given resource. The name of the
     * PropertySource will be generated based on the {@link Resource#getDescription() description}
     * of the given resource.
     */
    public ResourceTrimmingPropertySource( Resource resource ) throws IOException {
        this( getNameForResource( resource ), resource );
    }

    /**
     * Create a PropertySource having the given name based on Properties loaded from the given
     * resource location and using the given class loader to load the resource (assuming it is
     * prefixed with {@code classpath:}).
     */
    public ResourceTrimmingPropertySource( String name, String location, ClassLoader classLoader ) throws IOException {
        this( name, getResourceForLocation( location, classLoader ) );
    }

    /**
     * Create a PropertySource having the given name based on Properties loaded from the given
     * resource location. The default thread context class loader will be used to load the resource
     * (assuming the location string is prefixed with {@code classpath:}.
     */
    public ResourceTrimmingPropertySource( String name, String location ) throws IOException {
        this( name, location, ClassUtils.getDefaultClassLoader() );
    }

    /**
     * Create a PropertySource based on Properties loaded from the given resource location and use
     * the given class loader to load the resource, assuming it is prefixed with {@code classpath:}.
     * The name of the PropertySource will be generated based on the
     * {@link Resource#getDescription() description} of the resource.
     */
    public ResourceTrimmingPropertySource( String location, ClassLoader classLoader ) throws IOException {
        this( getResourceForLocation( location, classLoader ) );
    }

    /**
     * Create a PropertySource based on Properties loaded from the given resource location. The name
     * of the PropertySource will be generated based on the {@link Resource#getDescription()
     * description} of the resource.
     */
    public ResourceTrimmingPropertySource( String location ) throws IOException {
        this( getResourceForLocation( location, ClassUtils.getDefaultClassLoader() ) );
    }

    private static Resource getResourceForLocation( String location, ClassLoader classLoader ) {
        return new PathMatchingResourcePatternResolver( classLoader ).getResource( location );
    }

    private static Properties loadPropertiesForResource( Resource resource ) throws IOException {
        Properties props = new Properties();
        InputStream is = resource.getInputStream();
        props.load( is );
        try {
            is.close();
        }
        catch ( IOException ex ) {
            // ignore
        }

        // otrimovani hodnot
        for ( String name : props.stringPropertyNames() ) {
            String value = props.getProperty( name );
            props.put( name, org.apache.commons.lang3.StringUtils.trim( value ) );
        }

        return props;
    }

    /**
     * Returns the description string for the resource, and if empty returns the class name of the
     * resource plus its identity hash code.
     */
    private static String getNameForResource( Resource resource ) {
        String name = resource.getDescription();
        if ( !StringUtils.hasText( name ) ) {
            name = resource.getClass().getSimpleName() + "@" + System.identityHashCode( resource );
        }
        return name;
    }

}
