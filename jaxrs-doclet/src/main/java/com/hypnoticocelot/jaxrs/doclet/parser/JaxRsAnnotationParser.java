package com.hypnoticocelot.jaxrs.doclet.parser;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.hypnoticocelot.jaxrs.doclet.DocletOptions;
import com.hypnoticocelot.jaxrs.doclet.Recorder;
import com.hypnoticocelot.jaxrs.doclet.model.*;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.google.common.collect.Maps.uniqueIndex;

public class JaxRsAnnotationParser {

    private final DocletOptions options;
    private final RootDoc rootDoc;

    public JaxRsAnnotationParser(DocletOptions options, RootDoc rootDoc) {
        this.options = options;
        this.rootDoc = rootDoc;
        AnnotationHelper.setRootDoc(rootDoc);
    }

    public boolean run() {
        try {
            Collection<ApiDeclaration> declarations = new ArrayList<ApiDeclaration>();
            for (ClassDoc classDoc : rootDoc.classes()) {
                ApiClassParser classParser = new ApiClassParser(options, classDoc, Arrays.asList(rootDoc.classes()));
                Collection<Api> apis = classParser.parse();
                if (apis.isEmpty()) {
                    continue;
                }

                Map<String, Model> models = uniqueIndex(classParser.models(), new Function<Model, String>() {
                    public String apply(Model model) {
                        return model.getId();
                    }
                });
                // The idea (and need) for the declaration is that "/foo" and "/foo/annotated" are stored in separate
                // Api classes but are part of the same resource.
                String apiName = options.isEnableApiNames() ? classDoc.simpleTypeName() : null;
                declarations.add(new ApiDeclaration(apiName, options.getApiVersion(), options.getApiBasePath(), classParser.getRootPath(), apis, models));
            }
            writeApis(declarations);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void writeApis(Collection<ApiDeclaration> apis) throws IOException {
        List<ResourceListingAPI> resources = new LinkedList<ResourceListingAPI>();
        File outputDirectory = options.getOutputDirectory();
        Recorder recorder = options.getRecorder();
        for (ApiDeclaration api : apis) {
            String resourcePath = api.getResourcePath();
            if (!Strings.isNullOrEmpty(resourcePath)) {
                String resourceName = resourcePath.replaceFirst("/", "").replaceAll("/", "_").replaceAll("[\\{\\}]", "");
                resources.add(new ResourceListingAPI("/" + resourceName + ".{format}", ""));
                File apiFile = new File(outputDirectory, resourceName + ".json");
                recorder.record(apiFile, api);
            }
        }
    }

}
