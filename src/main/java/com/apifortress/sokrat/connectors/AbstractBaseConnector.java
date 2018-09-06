package com.apifortress.sokrat.connectors;

import com.apifortress.sokrat.Configuration;
import com.apifortress.sokrat.beans.WebHook;
import groovy.json.StringEscapeUtils;
import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * © 2018 Simone Pezzano
 *
 * @author Simone Pezzano
 **/
public abstract class AbstractBaseConnector {

    @Autowired
    Configuration configuration;

    final SimpleTemplateEngine ste;

    final Map<String,Template> templates;

    final HttpClient httpClient;

    public AbstractBaseConnector(){
        ste = new SimpleTemplateEngine();
        templates = new HashMap<String, Template>();
        httpClient = HttpClients.createMinimal(new PoolingHttpClientConnectionManager());
    }

    @PostConstruct
    public abstract void postConstruct() throws Exception;

    protected void makeCall(String message,WebHook webHook) throws Exception {
        final String payload = evaluateFormat(message,webHook.getFormat());
        final HttpPost post = new HttpPost(webHook.getUrl());
        post.setEntity(new StringEntity(payload));
        final HttpResponse response = httpClient.execute(post);
        final HttpEntity entity = response.getEntity();
        EntityUtils.consumeQuietly(entity);
        HttpClientUtils.closeQuietly(response);
    }

    private String evaluateFormat(String message, String format) throws IOException, ClassNotFoundException {
        if(format == null)
            return message;
        final String signature = DigestUtils.md5DigestAsHex(format.getBytes());
        Template template = templates.get(signature);
        if(template == null){
            template = ste.createTemplate(format);
            templates.put(signature,template);
        }
        Map<String,Object> scope = new HashMap<String, Object>();
        scope.put("payload",message);
        scope.put("StringEscapeUtils", StringEscapeUtils.class);
        StringWriter stringWriter = new StringWriter();
        template.make(scope).writeTo(stringWriter);
        return stringWriter.toString();
    }
}
