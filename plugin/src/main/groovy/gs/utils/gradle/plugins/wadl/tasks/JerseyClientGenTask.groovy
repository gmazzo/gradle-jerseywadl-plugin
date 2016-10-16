package gs.utils.gradle.plugins.wadl.tasks

import com.sun.codemodel.JClassAlreadyExistsException
import com.sun.codemodel.writer.FileCodeWriter
import org.apache.commons.lang.Validate
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.internal.file.collections.SimpleFileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jvnet.ws.wadl.ast.InvalidWADLException
import org.jvnet.ws.wadl2java.Wadl2Java

import javax.xml.bind.JAXBException
import java.util.logging.Logger

public class JerseyClientGenTask extends ConventionTask {
    private static final Logger LOGGER = Logger.getLogger(JerseyClientGenTask.class.getName());

    @InputFiles
    public FileCollection wadlFiles = new SimpleFileCollection();

    @Input
    public String packageName;

    @Input
    public boolean jersey2 = false;

    @Input
    public boolean autoPackage = true;

    @Input
    public Map<String, String> customClassNames = new HashMap<>();

    @InputFiles
    public FileCollection customizationsFiles = new SimpleFileCollection();

    @OutputDirectory
    public File outputDir;

    @TaskAction
    public void generateClientSources() throws IOException, JClassAlreadyExistsException, JAXBException, InvalidWADLException {
        Validate.notEmpty(wadlFiles, "No WADL files provided");
        Validate.notEmpty(packageName, "No root package provided");

        outputDir.mkdirs()

        Wadl2Java.Parameters parameters = new Wadl2Java.Parameters();
        parameters.setPkg(packageName);
        parameters.setGenerationStyle(jersey2 ? Wadl2Java.STYLE_JAXRS20 : Wadl2Java.STYLE_JERSEY1X);
        parameters.setAutoPackage(autoPackage);
        parameters.setCodeWriter(new FileCodeWriter(outputDir));
        parameters.setRootDir(outputDir.toURI());
        parameters.setCustomClassNames(customClassNames);
        parameters.setCustomizationsAsFiles(customizationsFiles.asList());

        Wadl2Java wadl2Java = new Wadl2Java(parameters);
        for (File file : wadlFiles.filter { it.exists() }) {
            LOGGER.info("Generating client classes from WADL " + file + " into " + outputDir);

            wadl2Java.process(file.toURI());
        }
    }

}

