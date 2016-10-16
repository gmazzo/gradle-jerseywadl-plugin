package gs.utils.gradle.plugins.wadl.tasks

import com.sun.research.ws.wadl.Include
import gs.utils.gradle.plugins.wadl.tasks.Generator.Callback
import org.glassfish.jersey.server.ApplicationHandler
import org.glassfish.jersey.server.ExtendedResourceContext
import org.glassfish.jersey.server.internal.JerseyResourceContext
import org.glassfish.jersey.server.wadl.internal.ApplicationDescription
import org.glassfish.jersey.server.wadl.internal.WadlApplicationContextImpl
import org.glassfish.jersey.servlet.ServletContainer

import javax.ws.rs.core.Application
import javax.ws.rs.core.UriInfo

class Jersey2Generator implements Generator {

    @Override
    public void buildApplication(WadlGenTask task, Class<?> appClass, Callback callback) throws Exception {
        UriInfo uriInfo = new UriInfoImpl(task.endpointURI);
        ApplicationHandler handler = getApplicationHandler(appClass);

        ExtendedResourceContext resourceContext = handler.serviceLocator.getService(JerseyResourceContext.class);
        WadlApplicationContextImpl context = new WadlApplicationContextImpl(
                handler.serviceLocator,
                handler.configuration,
                resourceContext);
        ApplicationDescription desc = context.getApplication(uriInfo, true);
        com.sun.research.ws.wadl.Application application = desc.getApplication()

        for (Include include : application.grammars.include) {
            String path = include.href = include.href.replaceFirst('^application.wadl/', '');

            def grammar = desc.getExternalGrammar(path);
            callback.storeObject(grammar.content, path);
        }

        callback.storeObject(application, null);
    }

    private ApplicationHandler getApplicationHandler(Class<?> appClass) {
        if (Application.class.isAssignableFrom(appClass)) {
            return new ApplicationHandler(appClass);

        } else if (ServletContainer.class.isAssignableFrom(appClass)) {
            ServletContainer app = appClass.newInstance();
            app.init();
            return app.applicationHandler;

        } else {
            throw new IllegalArgumentException("applicationClass[$appClass] is not an instance of ${Application.class}")
        }
    }

}
