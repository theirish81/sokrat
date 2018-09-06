package com.apifortress.sokrat.connectors;

import com.apifortress.sokrat.Configuration;
import com.apifortress.sokrat.beans.WebHook;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * © 2018 Simone Pezzano
 *
 * @author Simone Pezzano
 **/
@Component
public class KafkaConnector extends AbstractBaseConnector{

    public KafkaConnector(){
        super();
    }

    private static final Logger log = LoggerFactory.getLogger(KafkaConnector.class);

    private KafkaConnector(final Map<String,Object> connection, final Configuration configuration){
        this();
        log.debug("Creating a connection");
        this.configuration = configuration;
        final WebHook partialWebHook = new WebHook(configuration,connection);
        Properties props = new Properties();
        Map<String,String> kafkaProps = (Map<String,String>)connection.get("properties");
        props.putAll(kafkaProps);
        final KafkaConsumer<String, String> consumer = new KafkaConsumer<String,String>(props);
        new Thread(){
            public void run(){
                final List<String> topics = getTopicKeys(connection);
                log.info("Subscribing to topics : "+topics);
                consumer.subscribe(topics);
                try {
                    while (true) {
                        log.debug("Entered busy wait loop");
                        ConsumerRecords<String, String> records = consumer.poll(100);
                        for (ConsumerRecord<String, String> record : records) try {
                                makeCall(record.value(), partialWebHook.clone().apply(getTopicByTopicName(connection, record.topic())));
                            }catch(Exception e){ log.error("Error while making the HTTP call",e); }
                        }
                } finally {
                    consumer.close();
                }
            }
        }.start();
    }

    private static List<String> getTopicKeys(Map<String,Object> connection){
        return ((List<Map<String, Object>>) connection.get("topics"))
                .stream().map((Map<String,Object> m) -> (String) m.get("name")).collect(Collectors.toList());
    }

    private static Map<String,Object> getTopicByTopicName(Map<String, Object> connection, String topicName){
        return ((List<Map<String, Object>>) connection.get("topics"))
                .stream().filter(m -> m.get("name").equals(topicName)).findFirst().orElse(null);
    }

    public void postConstruct(){
        configuration = configuration.getConfiguration("kafka.yml");
        if(configuration != null ) {
            log.info("Starting Kafka connector");
            List<Map<String, Object>> connections = (List<Map<String, Object>>) configuration.get("connections");
            Iterator<Map<String, Object>> iterator = connections.iterator();
            while (iterator.hasNext()) {
                new KafkaConnector(iterator.next(), configuration);
            }
        }
    }
}
