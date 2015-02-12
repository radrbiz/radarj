package org.radarlab.client.kafka;

import org.radarlab.client.Config;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


public class AccountBalanceConsumer {
    private final Logger logger = Logger.getLogger(AccountBalanceConsumer.class);
    private final String groupId;
    private final String topic;
    private final Boolean loadFromStart;
    private ConsumerConnector consumer = null;
    protected ConsumerIterator<byte[], byte[]> consumerIterator;

    public AccountBalanceConsumer(String topic, String groupId, boolean loadFromStart) {
        this.groupId = groupId;
        this.topic = topic;
        this.loadFromStart = loadFromStart;
        startConsumer();
    }

    private void startConsumer(){
        synchronized (AccountBalanceConsumer.class) {
            boolean success = false;
            Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = null;
            while (!success) {
                try {
                    consumer = kafka.consumer.Consumer.createJavaConsumerConnector(createConsumerConfig());
                    Map<String, Integer> topicCountMap = new HashMap<>();
                    topicCountMap.put(topic, 1);
                    consumerMap = consumer.createMessageStreams(topicCountMap);
                    success = true;
                } catch (Exception ex) {
                    if (logger.isEnabledFor(Priority.ERROR)) {
                        logger.error("init cosumer stream error, restart connector.");
                    } else {
                        System.err.println("init cosumer stream error, restart connector.");
                    }
                    shutdown();
                }
            }
            List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(topic);
            consumerIterator = streams.get(0).iterator();
        }
    }

    public void shutdown() {
        if (consumer != null) consumer.shutdown();
    }

    public void run() {
    }

    private ConsumerConfig createConsumerConfig() {
        Properties props = new Properties();
        String cluster = Config.getInstance().getProperty("zookeeper.cluster");
        logger.info("kafka config zk cluster:" + cluster);
        props.put("zookeeper.connect", cluster);
        props.put("group.id", groupId);
        props.put("zookeeper.session.timeout.ms", "30000");
        props.put("zookeeper.sync.time.ms", "200");
//        props.put("rebalance.backoff.ms", "5000");
        props.put("auto.commit.interval.ms", "1000");
        if(loadFromStart)
            props.put("auto.offset.reset", "smallest");
        else
            props.put("auto.offset.reset", "largest");
        return new ConsumerConfig(props);
    }

    /**
     * get current zk offset value
     * /consumers/{group_id}/offsets/{topic}/{partition}
     * @return
     */
    public long getCurrentOffset(){
        String path = "/consumers/" + groupId + "/offsets/" + topic + "/0";
        String pathValue = ZookeeperClient.getPathValue(path);
        if(NumberUtils.isNumber(pathValue)){
            return Long.valueOf(pathValue);
        }else{
            return -1;
        }
    }

    /**
     * get current zk offset value
     * /consumers/{group_id}/offsets/{topic}/{partition}
     * @return
     */
    public void resetCurrentOffset(Long offset){
        shutdown();
        String path = "/consumers/" + groupId + "/offsets/" + topic + "/0";
        ZookeeperClient.setPathValue(path, String.valueOf(offset));
        startConsumer();
    }

    public void sleep(long miliseconds){
        try {
            Thread.sleep(miliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
