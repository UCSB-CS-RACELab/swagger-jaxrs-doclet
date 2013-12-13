package com.hypnoticocelot.jaxrs.doclet.parser;

import com.hypnoticocelot.jaxrs.doclet.DocletOptions;
import com.hypnoticocelot.jaxrs.doclet.model.*;
import com.hypnoticocelot.jaxrs.doclet.translator.Translator;
import com.sun.javadoc.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.Collections2.filter;
import static com.hypnoticocelot.jaxrs.doclet.parser.AnnotationHelper.parsePath;

public class ApiMethodParser {

    private static final String ANN_PRODUCES = "javax.ws.rs.Produces";
    private static final String ANN_CONSUMES = "javax.ws.rs.Consumes";
    private static final String RESPONSE_TYPE = "javax.ws.rs.core.Response";

    private final DocletOptions options;
    private final Translator translator;
    private final String parentPath;
    private final MethodDoc methodDoc;
    private final Set<Model> models;
    private final HttpMethod httpMethod;
    private final Method parentMethod;

    public ApiMethodParser(DocletOptions options, String parentPath, MethodDoc methodDoc) {
        this.options = options;
        this.translator = options.getTranslator();
        this.parentPath = parentPath;
        this.methodDoc = methodDoc;
        this.models = new LinkedHashSet<Model>();
        this.httpMethod = HttpMethod.fromMethod(methodDoc);
        this.parentMethod = null;
    }

    public ApiMethodParser(DocletOptions options, Method parentMethod, MethodDoc methodDoc) {
        this.options = options;
        this.translator = options.getTranslator();
        this.methodDoc = methodDoc;
        this.models = new LinkedHashSet<Model>();
        this.httpMethod = HttpMethod.fromMethod(methodDoc);
        this.parentPath = parentMethod.getPath();
        this.parentMethod = parentMethod;
    }

    public Method parse() {
        String methodPath = firstNonNull(parsePath(methodDoc.annotations()), "");
        if (httpMethod == null && methodPath.isEmpty()) {
            return null;
        }
        String path = parentPath + methodPath;

        // parameters
        List<ApiParameter> parameters = new LinkedList<ApiParameter>();
        for (Parameter parameter : methodDoc.parameters()) {
            if (!shouldIncludeParameter(httpMethod, parameter)) {
                continue;
            }
            if (options.isParseModels()) {
                models.addAll(new ApiModelParser(options, translator, parameter.type()).parse());
            }
            parameters.add(new ApiParameter(
                    AnnotationHelper.paramTypeOf(parameter),
                    AnnotationHelper.paramNameOf(parameter),
                    commentForParameter(methodDoc, parameter),
                    translator.typeName(parameter.type()).value()
            ));
        }

        // parent method parameters are inherited
        if (parentMethod != null)
            parameters.addAll(parentMethod.getParameters());

        // response messages
        Pattern pattern = Pattern.compile("(\\d+) (.+)"); // matches "<code><space><text>"
        List<ApiResponseMessage> responseMessages = new LinkedList<ApiResponseMessage>();
        for (String tagName : options.getErrorTags()) {
            for (Tag tagValue : methodDoc.tags(tagName)) {
                Matcher matcher = pattern.matcher(tagValue.text());
                if (matcher.find()) {
                    int code = Integer.valueOf(matcher.group(1));
                    String message = matcher.group(2);
                    String responseModel = null;
                    if (message != null) {
                        message = message.trim();
                        // if the message starts with {dataType} pattern...
                        if (message.indexOf('{') == 0 &&  message.indexOf('}') > 1) {
                            int end = message.indexOf('}');
                            String typeName = message.substring(1, end);
                            if (AnnotationHelper.isPrimitive(typeName)) {
                                responseModel = typeName;
                            } else {
                                Type responseType = AnnotationHelper.getClassDoc(typeName);
                                responseModel = translator.typeName(responseType).value();
                                if (options.isParseModels()) {
                                    models.addAll(new ApiModelParser(
                                            options, translator, responseType).parse());
                                }
                            }
                            message = message.substring(end + 1);
                        }
                    }
                    ApiResponseMessage responseMessage = new ApiResponseMessage(code, message);
                    responseMessage.setResponseModel(responseModel);
                    responseMessages.add(responseMessage);
                }
            }
        }

        // return type
        Type type = methodDoc.returnType();
        String returnType;
        if (RESPONSE_TYPE.equals(type.qualifiedTypeName())) {
            Tag[] outputTags = methodDoc.tags("output");
            if (outputTags == null || outputTags.length == 0) {
                throw new RuntimeException("@output tag is required for methods that return " +
                        "JAX-RS Response objects (" + methodDoc.containingClass().qualifiedTypeName() +
                        "." + methodDoc.name() + ")");
            }

            String outputType = outputTags[0].text();
            if (AnnotationHelper.isPrimitive(outputType)) {
                returnType = outputType;
            } else {
                ClassDoc clazzDoc = AnnotationHelper.getClassDoc(outputType);
                returnType = translator.typeName(clazzDoc).value();
            }
        } else {
            returnType = translator.typeName(type).value();
        }
        if (options.isParseModels()) {
            models.addAll(new ApiModelParser(options, translator, type).parse());
        }

        // First Sentence of Javadoc method description
        Tag[] fst = methodDoc.firstSentenceTags();
        StringBuilder sentences = new StringBuilder();
        for (Tag tag : fst) {
            sentences.append(tag.text());
        }
        String firstSentences = sentences.toString();

        Method method = new Method(
                httpMethod,
                methodDoc.name(),
                path,
                parameters,
                responseMessages,
                firstSentences,
                methodDoc.commentText().replace(firstSentences, ""),
                returnType
        );
        method.setProduces(getProduces(methodDoc));
        method.setConsumes(getConsumes(methodDoc));
        return method;
    }

    public Set<Model> models() {
        return models;
    }

    private boolean shouldIncludeParameter(HttpMethod httpMethod, Parameter parameter) {
        List<AnnotationDesc> allAnnotations = Arrays.asList(parameter.annotations());
        Collection<AnnotationDesc> excluded = filter(allAnnotations, new AnnotationHelper.ExcludedAnnotations(options));
        if (!excluded.isEmpty()) {
            return false;
        }

        Collection<AnnotationDesc> jaxRsAnnotations = filter(allAnnotations, new AnnotationHelper.JaxRsAnnotations());
        if (!jaxRsAnnotations.isEmpty()) {
            return true;
        }

        return (allAnnotations.isEmpty() || httpMethod == HttpMethod.POST);
    }

    private String commentForParameter(MethodDoc method, Parameter parameter) {
        for (ParamTag tag : method.paramTags()) {
            if (tag.parameterName().equals(parameter.name())) {
                return tag.parameterComment();
            }
        }
        return "";
    }

    private String[] getProduces(MethodDoc method) {
        AnnotationDesc[] annotations = method.annotations();
        for (AnnotationDesc ann : annotations) {
            String type = ann.annotationType().qualifiedName();
            if (ANN_PRODUCES.equals(type)) {
                String[] produces = new String[ann.elementValues().length];
                for (int i = 0; i < produces.length; i++) {
                    produces[i] = ann.elementValues()[i].value().toString();
                    produces[i] = produces[i].replaceAll("\"", "");
                }
                return produces;
            }
        }
        return null;
    }

    private String[] getConsumes(MethodDoc method) {
        for (Parameter parameter : methodDoc.parameters()) {
            if ("form".equals(AnnotationHelper.paramTypeOf(parameter))) {
                return new String[] { "application/x-www-form-urlencoded" };
            }
        }

        AnnotationDesc[] annotations = method.annotations();
        for (AnnotationDesc ann : annotations) {
            String type = ann.annotationType().qualifiedName();
            if (ANN_CONSUMES.equals(type)) {
                String[] produces = new String[ann.elementValues().length];
                for (int i = 0; i < produces.length; i++) {
                    produces[i] = ann.elementValues()[i].value().toString();
                    produces[i] = produces[i].replaceAll("\"", "");
                }
                return produces;
            }
        }
        return null;
    }

}
