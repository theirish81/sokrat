package com.apifortress.sokrat.beans;

import com.apifortress.sokrat.Configuration;

import java.util.Map;

/**
 * © 2018 Simone Pezzano
 *
 * @author Simone Pezzano
 **/
public class WebHook {

    private String url;
    private String format;

    public WebHook(Configuration configuration,Map<String,Object> subConfiguration){
        apply(configuration).apply(subConfiguration);
    }

    private WebHook(String url, String format){
        this.url = url;
        this.format = format;
    }

    public WebHook apply(Map<String,Object> subConfiguration){
        Map<String,String> hook = (Map<String, String>) subConfiguration.get("webhook");
        if(hook != null ){
            if(hook.containsKey("url"))
                url = hook.get("url");
            if(hook.containsKey("format"))
                format = hook.get("format");
        }
        return this;
    }

    public String getUrl(){
        return url;
    }

    public String getFormat(){
        return format;
    }

    public WebHook clone(){
        return new WebHook(url,format);
    }
}
