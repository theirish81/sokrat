package com.apifortress.sokrat;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 * © 2018 Simone Pezzano
 *
 * @author Simone Pezzano
 **/
@Component
public class Configuration extends HashMap<String,Object> {

    static Yaml yaml = new Yaml();

    public Configuration() throws FileNotFoundException {
        File directory = FileUtils.getFile("etc","modules.enabled");
        for(File f : directory.listFiles()){
            if(f.isFile() && f.getName().endsWith(".yml")) {
                FileReader reader = new FileReader(f);
                Map data = yaml.loadAs(reader, Map.class);
                put(f.getName(),data);
            }
        }
    }

    public Configuration(Map<String,Object> map){
        putAll(map);
    }
    public Map<String,Object> getMap(String key){
        return (Map<String, Object>) get(key);
    }

    public Configuration getConfiguration(String key){
        Map<String,Object> foundMap = getMap(key);
        if(foundMap == null)
            return null;
        return new Configuration(foundMap);
    }
}
