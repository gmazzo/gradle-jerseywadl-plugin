package gs.utils.gradle.plugins.wadl.tasks

import com.sun.jersey.api.core.DefaultResourceConfig
import com.sun.jersey.api.core.ResourceConfig
import com.sun.jersey.api.model.AbstractResource
import com.sun.jersey.api.uri.UriComponent
import com.sun.jersey.core.util.MultivaluedMapImpl
import com.sun.jersey.core.util.UnmodifiableMultivaluedMap
import com.sun.jersey.server.impl.modelapi.annotation.IntrospectionModeller
import com.sun.jersey.server.impl.wadl.WadlApplicationContextImpl
import com.sun.jersey.server.wadl.ApplicationDescription
import com.sun.jersey.spi.container.servlet.ServletContainer
import com.sun.jersey.spi.container.servlet.WebServletConfig
import org.apache.commons.lang.Validate
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import javax.ws.rs.core.*
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import javax.xml.bind.Marshaller
import java.util.logging.Logger

public class WadlGenTask extends ConventionTask {
    private static final Logger LOGGER = Logger.getLogger(WadlGenTask.class.getName());

    @Input
    public URI endpointURI;

    @Input
    public String applicationClass;

    @InputFiles
    public FileCollection applicationClasspath;

    @OutputFile
    public File wadlFile;

    @TaskAction
    public void generateWADLFile() throws JAXBException {
        Validate.notNull(endpointURI, "No default endpoint provided");
        Validate.notNull(applicationClass, "No applicationClass was provided");
        Validate.notNull(wadlFile, "No destination WADL file provided");

        LOGGER.info("Generating WADL for " + endpointURI + ", file " + wadlFile);

        UriInfo uriInfo = new UriInfoImpl(endpointURI);
        ResourceConfig resourceConfig = buildResourceConfig();

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
        com.sun.research.ws.wadl.Application application = desc.getApplication();

        wadlFile.getParentFile().mkdirs();

        JAXBContext jaxbContext = JAXBContext.newInstance(application.getClass());
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(application, wadlFile);
    }

    private ResourceConfig buildResourceConfig() {
        def classpath = applicationClasspath.collect { it.toURI().toURL() }
        def classLoader = new URLClassLoader(
                classpath.toArray(new URL[classpath.size()]),
                Thread.currentThread().getContextClassLoader())

        try {
            def appClass = classLoader.loadClass(applicationClass)

            if (Application.class.isAssignableFrom(appClass)) {
                Application app = appClass.newInstance();
                DefaultResourceConfig config = new DefaultResourceConfig();
                config.add(app)
                return config;

            } else if (ServletContainer.isAssignableFrom(appClass)) {
                ServletContainer app = appClass.newInstance()
                WebServletConfig config = new WebServletConfig(app)
                return app.getDefaultResourceConfig(Collections.emptyMap(), config)

            } else {
                throw new IllegalArgumentException("applicationClass[$applicationClass] is not an instance of ${Application.class}")
            }

        } catch (ClassNotFoundException e) {
            LOGGER.severe("Could not instantiate application class $applicationClass with classpath: $classpath")
            throw e
        }
    }

}

class UriInfoImpl implements UriInfo {
    private static
    final UnmodifiableMultivaluedMap<String, String> EMPTY_MULTY_MAP = new UnmodifiableMultivaluedMap<>(new MultivaluedMapImpl());
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

}
