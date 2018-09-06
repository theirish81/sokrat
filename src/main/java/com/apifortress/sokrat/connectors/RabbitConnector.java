package com.apifortress.sokrat.connectors;

import com.apifortress.sokrat.Configuration;
import com.apifortress.sokrat.beans.WebHook;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * © 2018 Simone Pezzano
 *
 * @author Simone Pezzano
 **/
@Component
public class RabbitConnector extends AbstractBaseConnector {

    @Autowired
    Configuration configuration;

    Connection rabbitConnection;

    private static final Logger log = LoggerFactory.getLogger(RabbitConnector.class);

    public RabbitConnector() {
        super();
    }

    public void postConstruct() throws Exception {
        configuration = configuration.getConfiguration("rabbitmq.yml");
        if(configuration != null ) {
            log.info("Starting RabbitMQ connector");
            List<Map> connections = ((List<Map>) configuration.get("connections"));
            Iterator<Map> cIterator = connections.iterator();
            while (cIterator.hasNext()) {
                new RabbitConnector(cIterator.next(), configuration);
            }
        }
    }

    private RabbitConnector(Map<String,Object> connection,Configuration configuration) throws NoSuchAlgorithmException, KeyManagementException, URISyntaxException, IOException, TimeoutException {
        this();
        log.debug("Creating a connection");
        this.configuration = configuration;
        ConnectionFactory factory = new ConnectionFactory();
        factory = factory.load((Map<String,String>)connection.get("properties"));
        Object useSsl = connection.get("use_ssl");
        if(useSsl != null ) {
            if (useSsl instanceof Boolean && useSsl.equals(true))
                factory.useSslProtocol();
            if (useSsl instanceof String)
                factory.useSslProtocol((String) useSsl);
        }
        List<Map> channels = (List<Map>) connection.get("channels");
        Iterator<Map> cIterator = channels.iterator();
        rabbitConnection = factory.newConnection();
        WebHook partialWebHook = new WebHook(configuration,connection);
        while(cIterator.hasNext()) {
            runChannel(new ChannelBean(cIterator.next()),partialWebHook);
        }
    }

    private void runChannel(ChannelBean channelBean,WebHook partialWebHook) throws IOException {
        log.debug("Creating channel: "+channelBean);
        String queue = channelBean.getQueue();
        Channel rabbitChannel = rabbitConnection.createChannel();
        if(channelBean.hasExchange()) {
            queue = rabbitChannel.queueDeclare().getQueue();
            rabbitChannel.queueBind(queue, channelBean.getExchangeName(), channelBean.getExchangeRoutingKey());
        }
        partialWebHook.apply(channelBean);
        rabbitChannel.basicConsume(queue,channelBean.isAutoAck(),channelBean.getTag(),new APIFortressConsumer(rabbitChannel,channelBean,partialWebHook));
    }

     class APIFortressConsumer extends DefaultConsumer {

         private ChannelBean channelBean;
         private WebHook webHook;

        public APIFortressConsumer(Channel channel,ChannelBean channelBean,WebHook webHook) {
            super(channel);
            this.webHook = webHook;
            this.channelBean = channelBean;
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body){
            try {
                makeCall(new String(body, "UTF-8"),webHook);
                if(!channelBean.isAutoAck())
                    getChannel().basicAck(envelope.getDeliveryTag(),false);
            }catch(Exception e){log.error("Error while consuming message",e);}
        }
    }

    class ChannelBean extends HashMap<String,Object>{

        public ChannelBean(Map<String,Object> map){
            putAll(map);
        }

        public String getQueue(){
            return (String) get("queue");
        }

        public String getTag(){
            return (String) get("tag");
        }

        public boolean isAutoAck(){
            Boolean autoAck = (Boolean) get("auto_ack");
            if(autoAck == null)
                autoAck = true;
            return autoAck;
        }

        public boolean hasExchange(){
            return containsKey("exchange");
        }
        public Map<String,Object> getExchange(){
            return (Map<String, Object>) get("exchange");
        }
        public String getExchangeName(){
            return (String) getExchange().get("name");
        }
        public String getExchangeRoutingKey(){
            return (String) getExchange().get("routing_key");
        }

        public String toString(){
            return "channel_"+get("name");
        }

    }
}
