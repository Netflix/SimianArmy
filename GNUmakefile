reformat:
	eclipse -nosplash -application org.eclipse.jdt.core.JavaCodeFormatter -verbose -config $(shell pwd)/codequality/org.eclipse.jdt.core.prefs $(shell pwd)/src
	find $(shell pwd)/src -name \*.java | xargs perl -pi -e 's/{ /{/g; s/(\S) }/$$1}/g; s/\* $$/\*/; s/([.]<[^>]+>)\s+/$$1/g'
