package gs.utils.gradle.plugins.wadl

import gs.utils.gradle.plugins.wadl.tasks.Wadl1GenTask
import gs.utils.gradle.plugins.wadl.tasks.WadlGenTask

public class Jersey1WadlPlugin extends JerseyWadlPlugin {

    @Override
    protected String getTargetServletClass() {
        return 'com.sun.jersey.spi.container.servlet.ServletContainer'
    }

    @Override
    protected String getDefaultClientDependency() {
        return 'com.sun.jersey:jersey-client:1.19.2';
    }

    @Override
    protected Class<? extends WadlGenTask> getWadlGenTaskType() {
        return Wadl1GenTask.class
    }

}
