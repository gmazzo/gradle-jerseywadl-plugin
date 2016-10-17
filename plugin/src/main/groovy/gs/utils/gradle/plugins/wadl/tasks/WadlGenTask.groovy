package gs.utils.gradle.plugins.wadl.tasks

import org.apache.commons.lang.Validate
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import java.util.logging.Logger

public abstract class WadlGenTask<A, G> extends ConventionTask {
    protected final Logger LOGGER = Logger.getLogger(getClass().getName());

    @Input
    public URI endpointURI;

    @Input
    public String applicationClass;

    @Input
    public String wadlFileName;

    @InputFiles
    public FileCollection applicationClasspath;

    @OutputDirectory
    public File wadlOutput;

    protected abstract void processApplication(Class<?> appClass);

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

            processApplication(appClass)

        } catch (ClassNotFoundException e) {
            LOGGER.severe("Could not instantiate application class $applicationClass with classpath: $classpath")
            throw e
        }
    }

    protected void storeApplication(A application) {
        File file = new File(wadlOutput, wadlFileName);
        JAXBContext jaxbContext = JAXBContext.newInstance(application.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(application, file);
    }

    protected void storeGrammar(G grammar, String path) {
        File file = new File(wadlOutput, path);
        file.withOutputStream {
            it.write(grammar.content)
        }
    }

}
