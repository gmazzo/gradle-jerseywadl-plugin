package gs.utils.gradle.plugins.wadl

import gs.utils.gradle.plugins.wadl.tasks.JerseyClientGenTask
import gs.utils.gradle.plugins.wadl.tasks.WadlGenTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.collections.SimpleFileCollection
import org.gradle.jvm.tasks.Jar

public abstract class JerseyWadlPlugin implements Plugin<Project> {
    final static String GROUP_NAME = 'Jersey WADL & Client Generation'

    WadlGenPluginExtension wadlGen
    JerseyGenPluginExtension jerseyGen
    WadlGenTask wadlTask
    JerseyClientGenTask clientTask
    Jar packClientTask

    protected abstract String getTargetServletClass();

    protected abstract String getDefaultClientDependency();

    protected abstract Class<? extends WadlGenTask> getWadlGenTaskType();

    void apply(Project project) {
        createConfigureWadlGenTask(project)
        createWadlGenTask(project)
        createJerseyClientGenTask(project)

        project.sourceSets {
            jerseyClient {
                java {
                    srcDir clientTask.outputDir
                }
            }
        }

        createPackJerseyClientTask(project)

        project.processJerseyClientResources.dependsOn wadlTask
        project.compileJerseyClientJava.dependsOn clientTask

        project.afterEvaluate {
            if (jerseyGen.includeJerseyClientDependency) {
                project.dependencies {
                    jerseyClientCompile jerseyGen.customJerseyClientDependency ?: getDefaultClientDependency()
                }
            }
        }

        project.clean << {
            delete wadlTask.outputs
            delete clientTask.outputs
            delete packClientTask.outputs
        }
    }

    Task createConfigureWadlGenTask(Project project) {
        project.afterEvaluate {
            if (project.plugins.hasPlugin('war')) {
                FileCollection webXmlFile = project.files(project.convention.plugins['war'].webAppDir).asFileTree.matching({
                    include 'WEB-INF/web.xml'
                })

                Task retrieveAppTask = project.task('configureGenerateWADL', group: GROUP_NAME, description: 'Sets applicationClass by reading WEB-INF/web.xml file');
                retrieveAppTask.onlyIf { !wadlTask.applicationClass && !webXmlFile.isEmpty() }
                retrieveAppTask << {
                    def file = webXmlFile.singleFile
                    def targetServlet = getTargetServletClass()
                    def xml = new XmlSlurper().parse(file)
                    xml.declareNamespace('': 'http://java.sun.com/xml/ns/javaee')

                    wadlGen.applicationClass = xml.servlet
                            .find({ it.'servlet-class'.text().equals(targetServlet) })
                            .'init-param'
                            .find({ it.'param-name'.text().equals('javax.ws.rs.Application') })
                            .'param-value'
                }
                retrieveAppTask.dependsOn project.processResources
                wadlTask.dependsOn retrieveAppTask
            }
        }
    }

    WadlGenTask createWadlGenTask(Project project) {
        wadlGen = project.extensions.create("wadlGen", WadlGenPluginExtension)
        wadlGen.applicationClasspath = project.sourceSets.main.output +
                project.sourceSets.main.compileClasspath +
                project.sourceSets.main.runtimeClasspath
        wadlGen.wadlOutputFolder = new File(project.buildDir, 'generated/wadl')
        wadlGen.wadlOutputFileName = 'application.wadl'

        wadlTask = project.task('generateWADL', type: getWadlGenTaskType(), group: GROUP_NAME, description: 'Generates a WADL file from the Jersey app class');
        wadlTask.onlyIf { wadlGen.applicationClass }
        wadlTask.doFirst {
            endpointURI = wadlGen.endpointURI
            applicationClass = wadlGen.applicationClass
            applicationClasspath = wadlGen.applicationClasspath
            wadlOutput = wadlGen.wadlOutputFolder
            wadlFileName = wadlGen.wadlOutputFileName
        }
        wadlTask.dependsOn project.compileJava
        return wadlTask
    }

    JerseyClientGenTask createJerseyClientGenTask(Project project) {
        jerseyGen = project.extensions.create("jerseyGen", JerseyGenPluginExtension)
        jerseyGen.clientPackage = "${project.group}.${project.name}.client".replaceAll('[^\\w_.]', '_').replaceAll('(^|\\.)(\\d)', '$1_$2');
        jerseyGen.wadlFiles += project.fileTree(wadlGen.wadlOutputFolder).matching({
            include '**.wadl'
        })
        jerseyGen.wadlFiles += project.fileTree('src/main/wadl').matching({
            include '**.wadl'
        })

        clientTask = project.task('generateJerseyClient', type: JerseyClientGenTask, group: GROUP_NAME, description: 'Generates Jersey client classes');
        clientTask.onlyIf { !jerseyGen.wadlFiles.empty }
        clientTask.packageName = jerseyGen.clientPackage
        clientTask.jersey2 = this instanceof Jersey2WadlPlugin;
        clientTask.wadlFiles += jerseyGen.wadlFiles
        clientTask.outputDir = new File(project.buildDir, 'generated/source/jersey')
        clientTask.doFirst {
            packageName = jerseyGen.clientPackage
            customClassNames = jerseyGen.customClassNames
            customizationsFiles = jerseyGen.customizationsFiles
        }
        clientTask.dependsOn wadlTask
        return clientTask
    }

    Jar createPackJerseyClientTask(Project project) {
        packClientTask = project.task('packJerseyClient', type: Jar, group: GROUP_NAME, description: 'Packages the generated Jersey client in a JAR library');
        packClientTask.from project.sourceSets.jerseyClient.output
        packClientTask.classifier 'jerseyClient'
        packClientTask.onlyIf {
            !packClientTask.source.asFileTree.matching({
                exclude 'MANIFEST.MF'
                include { it.file.exists() }
            }).empty
        }
        packClientTask.dependsOn project.jerseyClientClasses

        project.configurations {
            jerseyClient {
                extendsFrom jerseyClientCompile
            }
        }
        project.artifacts {
            jerseyClient packClientTask
        }
    }

}

class WadlGenPluginExtension {

    URI endpointURI;

    String applicationClass;

    File wadlOutputFolder;

    String wadlOutputFileName;

    FileCollection applicationClasspath = new SimpleFileCollection();

    void endpointURI(String uri) {
        this.endpointURI = URI.create(uri)
    }

}

class JerseyGenPluginExtension {

    String clientPackage;

    FileCollection wadlFiles = new SimpleFileCollection();

    Map<String, String> customClassNames = new HashMap<>();

    FileCollection customizationsFiles = new SimpleFileCollection();

    String customJerseyClientDependency;

    boolean includeJerseyClientDependency = true;

}
