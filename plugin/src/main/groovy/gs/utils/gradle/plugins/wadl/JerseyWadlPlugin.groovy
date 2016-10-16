package gs.utils.gradle.plugins.wadl

import gs.utils.gradle.plugins.wadl.tasks.JerseyClientGenTask
import gs.utils.gradle.plugins.wadl.tasks.WadlGenTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.collections.SimpleFileCollection
import org.gradle.jvm.tasks.Jar

public class JerseyWadlPlugin implements Plugin<Project> {
    final static String GROUP_NAME = 'Jersey WADL & Client Generation'

    WadlGenPluginExtension wadlGen
    JerseyGenPluginExtension jerseyGen
    WadlGenTask wadlTask
    JerseyClientGenTask clientTask
    Jar packClientTask

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
                    def xml = new XmlSlurper().parse(file)
                    xml.declareNamespace('': 'http://java.sun.com/xml/ns/javaee')

                    def appParam = xml.'*'.'init-param'.find({
                        it.'param-name'.toString().equals('javax.ws.rs.Application')
                    })
                    def servletClass = appParam.'..'.'servlet-class'.toString()

                    wadlTask.jersey2 = clientTask.jersey2 =
                            'org.glassfish.jersey.servlet.ServletContainer'.equals(servletClass)
                    wadlGen.applicationClass = appParam.'param-value'.toString()
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

        wadlTask = project.task('generateWADL', type: WadlGenTask, group: GROUP_NAME, description: 'Generates a WADL file from the Jersey app class');
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
        clientTask.onlyIf { jerseyGen.clientPackage }
        clientTask.packageName = jerseyGen.clientPackage
        clientTask.wadlFiles += jerseyGen.wadlFiles
        clientTask.outputDir = new File(project.buildDir, 'generated/source/jersey')
        clientTask.doFirst {
            packageName = jerseyGen.clientPackage
            customClassNames = jerseyGen.customClassNames
            customizationsFiles = jerseyGen.customizationsFiles
        }
        clientTask.doLast {
            if (jerseyGen.includeJerseyClientDependency) {
                project.dependencies.add('jerseyClientCompile',
                        jerseyGen.customJerseyClientDependency ?: clientTask.jersey2 ?
                                'org.glassfish.jersey.containers:jersey-container-servlet-core:2.23.2' :
                                'com.sun.jersey:jersey-client:1.19.2'
                )
            }
        }
        clientTask.dependsOn wadlTask
        return clientTask
    }

    Jar createPackJerseyClientTask(Project project) {
        packClientTask = project.task('packJerseyClient', type: Jar, group: GROUP_NAME, description: 'Packages the generated Jersey client in a JAR library');
        packClientTask.from project.sourceSets.jerseyClient.output
        packClientTask.classifier 'jerseyClient'
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
