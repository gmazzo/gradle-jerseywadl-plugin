package gs.utils.gradle.plugins.wadl.tasks

import com.sun.jersey.core.util.MultivaluedMapImpl
import com.sun.jersey.core.util.UnmodifiableMultivaluedMap
import org.apache.commons.lang.Validate
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.PathSegment
import javax.ws.rs.core.UriBuilder
import javax.ws.rs.core.UriInfo
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import java.util.logging.Logger

public class WadlGenTask extends ConventionTask {
    private static final Logger LOGGER = Logger.getLogger(WadlGenTask.class.getName());

    @Input
    public URI endpointURI;

    @Input
    public String applicationClass;

    @Input
    public boolean jersey2;

    @Input
    public String wadlFileName;

    @InputFiles
    public FileCollection applicationClasspath;

    @OutputDirectory
    public File wadlOutput;

    @TaskAction
    public void generateWADLFile() throws Exception {
        Validate.notNull(endpointURI, "No default endpoint provided");
        Validate.notNull(applicationClass, "No applicationClass was provided");
        Validate.notNull(wadlOutput, "No destination WADL folder provided");
        Validate.notNull(wadlFileName, "No destination WADL file name provided");

        LOGGER.info("Generating WADL for " + endpointURI + ", file " + new File(wadlOutput, wadlFileName));

        def classpath = applicationClasspath.collect {
            it.toURI().toURL()
        }
        def classLoader = new URLClassLoader(
                classpath.toArray(new URL[classpath.size()]),
                Thread.currentThread().getContextClassLoader())

        try {
            Class<?> appClass = classLoader.loadClass(applicationClass)

            wadlOutput.mkdirs()

            Generator generator = jersey2 ? new Jersey2Generator() : new Jersey1Generator();
            generator.buildApplication(this, appClass, { object, path ->
                File outLocation = new File(wadlOutput, path ?: wadlFileName);

                if (object instanceof byte[]) {
                    outLocation.withOutputStream {
                        it.write((byte[]) object)
                    }

                } else {
                    JAXBContext jaxbContext = JAXBContext.newInstance(object.class);
                    Marshaller marshaller = jaxbContext.createMarshaller();
                    marshaller.marshal(object, outLocation);
                }
            })

        } catch (ClassNotFoundException e) {
            LOGGER.severe("Could not instantiate application class $applicationClass with classpath: $classpath")
            throw e
        }
    }

}

interface Generator {

    void buildApplication(WadlGenTask task, Class<?> appClass, Callback callback) throws Exception

    interface Callback {

        void storeObject(Object object, String path);

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
