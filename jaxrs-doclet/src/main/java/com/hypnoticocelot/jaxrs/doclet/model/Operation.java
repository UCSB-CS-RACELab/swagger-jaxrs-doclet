package com.hypnoticocelot.jaxrs.doclet.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.hypnoticocelot.jaxrs.doclet.parser.AnnotationHelper;

import java.util.List;

import static com.google.common.base.Strings.emptyToNull;

public class Operation {

    private HttpMethod method;
    private String nickname;
    private String type; // void, primitive, complex or a container
    private List<ApiParameter> parameters;
    private String summary; // cap at 60 characters for readability in the UI
    private String notes;
    private String[] produces;
    private String[] consumes;

    private List<ApiResponseMessage> responseMessages; // swagger 1.2 name

    @SuppressWarnings("unused")
    private Operation() {
    }

    public Operation(Method method) {
        this.method = method.getMethod();
        this.nickname = emptyToNull(method.getMethodName());
        this.type = emptyToNull(AnnotationHelper.typeOf(method.getReturnType()));
        this.parameters = method.getParameters().isEmpty() ? null : method.getParameters();
        this.responseMessages = method.getResponseMessages().isEmpty() ? null : method.getResponseMessages();
        this.summary = emptyToNull(method.getFirstSentence());
        this.notes = emptyToNull(method.getComment());
        this.produces = method.getProduces();
        this.consumes = method.getConsumes();
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getNickname() {
        return nickname;
    }

    public String getType() {
        return type;
    }

    public List<ApiParameter> getParameters() {
        return parameters;
    }
    
    public List<ApiResponseMessage> getResponseMessages() {
        return responseMessages;
    }

    public String getSummary() {
        return summary;
    }

    public String getNotes() {
        return notes;
    }

    public String[] getProduces() {
        return produces;
    }

    public String[] getConsumes() {
        return consumes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Operation that = (Operation) o;
        return Objects.equal(method, that.method)
                && Objects.equal(nickname, that.nickname)
                && Objects.equal(type, that.type)
                && Objects.equal(parameters, that.parameters)
                && Objects.equal(responseMessages, that.responseMessages)
                && Objects.equal(summary, that.summary)
                && Objects.equal(notes, that.notes);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(method, nickname, type, parameters, responseMessages, summary, notes);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("httpMethod", method)
                .add("nickname", nickname)
                .add("responseClass", type)
                .add("parameters", parameters)
                .add("responseMessages", responseMessages)
                .add("summary", summary)
                .add("notes", notes)
                .toString();
    }
}
