package gs.utils.gradle.plugins.wadl.tasks

import com.sun.jersey.api.core.DefaultResourceConfig
import com.sun.jersey.api.core.ResourceConfig
import com.sun.jersey.api.model.AbstractResource
import com.sun.jersey.core.util.MultivaluedMapImpl
import com.sun.jersey.core.util.UnmodifiableMultivaluedMap
import com.sun.jersey.server.impl.modelapi.annotation.IntrospectionModeller
import com.sun.jersey.server.impl.wadl.WadlApplicationContextImpl
import com.sun.jersey.server.wadl.ApplicationDescription
import com.sun.jersey.spi.container.servlet.ServletContainer
import com.sun.jersey.spi.container.servlet.WebServletConfig
import com.sun.research.ws.wadl.Include

import javax.ws.rs.core.*

public class Wadl1GenTask extends WadlGenTask<com.sun.research.ws.wadl.Application, ApplicationDescription.ExternalGrammar> {

    private static ResourceConfig getResourceConfig(Class<?> appClass) {
        if (Application.class.isAssignableFrom(appClass)) {
            Application app = appClass.newInstance();
            DefaultResourceConfig config = new DefaultResourceConfig();
            config.add(app)
            return config;

        } else if (ServletContainer.class.isAssignableFrom(appClass)) {
            ServletContainer app = appClass.newInstance()
            WebServletConfig config = new WebServletConfig(app)
            return app.getDefaultResourceConfig(Collections.emptyMap(), config)

        } else {
            throw new IllegalArgumentException("applicationClass[$appClass] is not an instance of ${Application.class}")
        }
    }

    @Override
    protected void processApplication(Class<?> appClass) {
        UriInfo uriInfo = new UriInfoImpl(endpointURI);
        ResourceConfig resourceConfig = getResourceConfig(appClass);

        Set<AbstractResource> roots = new LinkedHashSet<>();
        for (Object resource : resourceConfig.getRootResourceSingletons()) {
            AbstractResource ar = IntrospectionModeller.createResource(resource.getClass());

            LOGGER.info("Adding resource " + ar);
            roots.add(ar);
        }
        for (Class<?> resourceClass : resourceConfig.getRootResourceClasses()) {
            AbstractResource ar = IntrospectionModeller.createResource(resourceClass);

            LOGGER.info("Adding resource " + ar);
            roots.add(ar);
        }

        WadlApplicationContextImpl context = new WadlApplicationContextImpl(roots, resourceConfig, null);
        ApplicationDescription desc = context.getApplication(uriInfo);
        com.sun.research.ws.wadl.Application application = desc.getApplication()

        for (Include include : application.grammars.include) {
            String path = include.href = include.href.replaceFirst('^application.wadl/', '');

            def grammar = desc.getExternalGrammar(path);
            storeGrammar(grammar, path)
        }

        storeApplication(application)
    }

}

class UriInfoImpl implements UriInfo {
    private static final UnmodifiableMultivaluedMap<String, String> EMPTY_MULTY_MAP =
            new UnmodifiableMultivaluedMap<>(new MultivaluedMapImpl());
    private final URI uri;

    UriInfoImpl(URI uri) {
        this.uri = uri;
    }

    @Override
    public String getPath() {
        return getPath(true);
    }

    @Override
    public String getPath(boolean decode) {
        return decode ? uri.getPath() : uri.getRawPath();
    }

    @Override
    public List<PathSegment> getPathSegments() {
        return getPathSegments(true);
    }

    @Override
    public List<PathSegment> getPathSegments(boolean decode) {
        return UriComponent.decodePath(getPath(false), decode);
    }

    @Override
    public URI getRequestUri() {
        return uri;
    }

    @Override
    public UriBuilder getRequestUriBuilder() {
        return getAbsolutePathBuilder();
    }

    @Override
    public URI getAbsolutePath() {
        return uri;
    }

    @Override
    public UriBuilder getAbsolutePathBuilder() {
        return UriBuilder.fromUri(uri);
    }

    @Override
    public URI getBaseUri() {
        return uri;
    }

    @Override
    public UriBuilder getBaseUriBuilder() {
        return getAbsolutePathBuilder();
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters() {
        return EMPTY_MULTY_MAP;
    }

    @Override
    public MultivaluedMap<String, String> getPathParameters(boolean decode) {
        return EMPTY_MULTY_MAP;
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters() {
        return EMPTY_MULTY_MAP;
    }

    @Override
    public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
        return EMPTY_MULTY_MAP;
    }

    @Override
    public List<String> getMatchedURIs() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getMatchedURIs(boolean decode) {
        return Collections.emptyList();
    }

    @Override
    public List<Object> getMatchedResources() {
        return Collections.emptyList();
    }

    @Override
    public URI resolve(URI uri) {
        return uri;
    }

    @Override
    public URI relativize(URI uri) {
        return uri;
    }

}
