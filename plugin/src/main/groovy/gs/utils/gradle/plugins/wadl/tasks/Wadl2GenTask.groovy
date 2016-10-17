package gs.utils.gradle.plugins.wadl.tasks

import com.sun.research.ws.wadl.Include
import org.glassfish.jersey.server.ApplicationHandler
import org.glassfish.jersey.server.ExtendedResourceContext
import org.glassfish.jersey.server.internal.JerseyResourceContext
import org.glassfish.jersey.server.wadl.internal.ApplicationDescription
import org.glassfish.jersey.server.wadl.internal.WadlApplicationContextImpl
import org.glassfish.jersey.servlet.ServletContainer

import javax.ws.rs.core.Application
import javax.ws.rs.core.UriInfo

public class Wadl2GenTask extends WadlGenTask<com.sun.research.ws.wadl.Application, ApplicationDescription.ExternalGrammar> {

    private static ApplicationHandler getApplicationHandler(Class<?> appClass) {
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

    @Override
    protected void processApplication(Class<?> appClass) {
        UriInfo uriInfo = new UriInfoImpl(endpointURI);
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
            storeGrammar(grammar, path)
        }

        storeApplication(application)
    }

}

