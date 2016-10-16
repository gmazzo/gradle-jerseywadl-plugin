package gs.utils.gradle.plugins.wadl.tasks

import com.sun.jersey.api.core.DefaultResourceConfig
import com.sun.jersey.api.core.ResourceConfig
import com.sun.jersey.api.model.AbstractResource
import com.sun.jersey.server.impl.modelapi.annotation.IntrospectionModeller
import com.sun.jersey.server.impl.wadl.WadlApplicationContextImpl
import com.sun.jersey.server.wadl.ApplicationDescription
import com.sun.jersey.spi.container.servlet.ServletContainer
import com.sun.jersey.spi.container.servlet.WebServletConfig
import com.sun.research.ws.wadl.Include
import gs.utils.gradle.plugins.wadl.tasks.Generator.Callback

import javax.ws.rs.core.Application
import javax.ws.rs.core.UriInfo
import java.util.logging.Logger

class Jersey1Generator implements Generator {
    private static final Logger LOGGER = Logger.getLogger(Jersey1Generator.class.getName());

    @Override
    public void buildApplication(WadlGenTask task, Class<?> appClass, Callback callback) throws Exception {
        UriInfo uriInfo = new UriInfoImpl(task.endpointURI);
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
            callback.storeObject(grammar.content, path);
        }

        callback.storeObject(application, null);
    }

    private ResourceConfig getResourceConfig(Class<?> appClass) {
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

}
