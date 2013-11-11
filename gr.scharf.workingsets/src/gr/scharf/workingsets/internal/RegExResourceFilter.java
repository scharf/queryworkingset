package gr.scharf.workingsets.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IWorkingSet;

public class RegExResourceFilter implements IResourceProxyVisitor {
	class RegexIncludeMatcher implements ResournceMatcher {
		protected final Pattern pattern;

		RegexIncludeMatcher(String filter) {
			this.pattern = Pattern.compile(filter);
		}

		@Override
		public void  matchResource(IResourceProxy proxy, State state) {
			if(pattern.matcher(proxy.requestFullPath().toString()).find()) {
				state.include();
			}
		}
		public boolean isExclusion() {
			return false;
		}
	}
	class RegexExcludeMatcher extends RegexIncludeMatcher {
		RegexExcludeMatcher(String filter) {
			super(filter);
		}

		@Override
		public void  matchResource(IResourceProxy proxy, State state) {
			if(pattern.matcher(proxy.requestFullPath().toString()).find()) {
				state.exclude();
			}
		}
		public boolean isExclusion() {
			return true;
		}
		
	}
	
	class State {
		boolean include;
		boolean exclude;

		public boolean isInclude() {
			return include;
		}
		public boolean isExclude() {
			return exclude;
		}

		public void include() {
			this.include = true;
		}

		public void exclude() {
			this.exclude = true;
		}
		
	}
	interface ResournceMatcher {
		void matchResource(IResourceProxy proxy, State state);
	}
	boolean fInclude;
	final ResournceMatcher[] fMatchers;
	final private Collection<IAdaptable> fResources;
	private IWorkingSet workingSet;
	private boolean fModified;
	
	public RegExResourceFilter(IWorkingSet workingSet) {
		this(workingSet.getName().substring(7), Arrays.asList((IAdaptable[]) workingSet.getElements()));
		this.workingSet=workingSet;
	}
	public RegExResourceFilter(String filterString, Collection<IAdaptable> initialResources) {
		this.fResources=new HashSet<IAdaptable>();
		if(initialResources!=null)
			this.fResources.addAll(initialResources);
		// see http://stackoverflow.com/questions/1247772/is-there-an-equivalent-of-java-util-regex-for-glob-type-patterns
		String[] filters = filterString.split("\\s*\\n\\s*");
		List<ResournceMatcher> matchers=new ArrayList<ResournceMatcher>();
		for (String filter : filters) {
			ResournceMatcher matcher =null;
			if(filter.startsWith("-")) {
				filter=filter.substring(1);
				matcher = new RegexExcludeMatcher(filter);
			} else if(filter.startsWith("#")){
				// this is a comment -- ignore it
			} else if(filter.length()>0){
				matcher = new RegexIncludeMatcher(filter);
			}
			if(matcher!=null)
				matchers.add(matcher);
		}
		// we reverse the collection to be able to prune the search if we find
		// a matcher that is a exclusion matcher
		Collections.reverse(matchers);
		fMatchers = matchers.toArray(new ResournceMatcher[matchers.size()]);
	}
	public Collection<IAdaptable> getResult() {
		return fResources;
	}
	@Override
	public boolean visit(IResourceProxy proxy) throws CoreException {
		if(proxy.getType() != IResource.FILE)
			return true;
		State state = new State();
		// Note: we iterate in reverse order of the way the user entered  the filter
		// because the list has been reversed....
		for (int i = 0; i < fMatchers.length; i++) {
			ResournceMatcher matcher = fMatchers[i];
			matcher.matchResource(proxy, state);
			if(state.isExclude()) {
				// the filters above have no effect
				break;
			} else if(state.isInclude()) {
				// because we iterate in reverse order, the an include cannot be hidden
				// by an exclude
				doAddResource(proxy.requestResource());
				break;
			}
		}
		return true;
	}
	private void doAddResource(IResource resource) {
//		System.out.println("add " + !fResources.contains(resource) + " " + resource);
		if(fResources.add(resource)) {
			fModified=true;
		}
	}
	private void doRemoveResource(IResource resource) {
//		System.out.println("rem " + fResources.contains(resource) + " " + resource);
		if(fResources.remove(resource)) {
			fModified = true;
		}
	}
	public void endUpdate() {
		if(fModified) {
			fModified = false;
			workingSet.setElements((IAdaptable[])fResources.toArray(new IAdaptable[fResources.size()]));
		}
	}
	public void addResource(IResourceProxy proxy) throws CoreException {
		visit(proxy);
	}
	public void removeResource(IResource resource) throws CoreException {
		doRemoveResource(resource);
	}
	public void updateResource(IResourceProxy proxy) throws CoreException {
		removeResource(proxy.requestResource());
		addResource(proxy);
	}

}
