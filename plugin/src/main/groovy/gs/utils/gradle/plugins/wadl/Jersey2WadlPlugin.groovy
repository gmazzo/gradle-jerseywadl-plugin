package gs.utils.gradle.plugins.wadl

import gs.utils.gradle.plugins.wadl.tasks.Wadl2GenTask
import gs.utils.gradle.plugins.wadl.tasks.WadlGenTask

public class Jersey2WadlPlugin extends JerseyWadlPlugin {

    @Override
    protected String getTargetServletClass() {
        return 'org.glassfish.jersey.servlet.ServletContainer'
    }

    @Override
    protected String getDefaultClientDependency() {
        return 'org.glassfish.jersey.core:jersey-client:2.23.2'
    }

    @Override
    protected Class<? extends WadlGenTask> getWadlGenTaskType() {
        return Wadl2GenTask.class
    }

}
