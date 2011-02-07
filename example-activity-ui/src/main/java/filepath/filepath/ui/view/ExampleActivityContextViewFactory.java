package filepath.filepath.ui.view;

import java.util.Arrays;
import java.util.List;

import net.sf.taverna.t2.workbench.ui.views.contextualviews.ContextualView;
import net.sf.taverna.t2.workbench.ui.views.contextualviews.activity.ContextualViewFactory;

import filepath.filepath.ExampleActivity;

public class ExampleActivityContextViewFactory implements
		ContextualViewFactory<ExampleActivity> {

	public boolean canHandle(Object selection) {
		return selection instanceof ExampleActivity;
	}

	public List<ContextualView> getViews(ExampleActivity selection) {
		return Arrays.<ContextualView>asList(new ExampleContextualView(selection));
	}
	
}
