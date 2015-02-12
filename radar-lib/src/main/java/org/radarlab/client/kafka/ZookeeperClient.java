package org.radarlab.client.kafka;

import org.radarlab.client.Config;
import org.apache.log4j.Logger;
import org.apache.zookeeper.*;

import java.io.IOException;


public class ZookeeperClient {
    private static final Logger log = Logger.getLogger(ZookeeperClient.class);
    public static ZooKeeper zk;
    private static String zkCluster;
    private static final String rootPath = Config.getInstance().getProperty("zookeeper.ledger_index");

    static {
        zkCluster = Config.getInstance().getProperty("zookeeper.cluster");
        log.info("zkCluster from config: " + zkCluster);

        if (zk == null) {
            synchronized (ZooKeeper.class) {
                if (zk == null) {
                    try {
                        zk = new ZooKeeper(zkCluster, 30000, new SessionWatcher());

                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {

                    }
                }
            }
        }
    }

    public static int getLedgerIndex() {
        try {
            if (zk.exists(rootPath, false) == null) {
                return 0;
            } else {
                return Integer.valueOf(new String(zk.getData(rootPath, false, null)));
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static String getPathValue(String path) {
        try {
            if (zk.exists(path, false) == null) {
                return null;
            } else {
                return new String(zk.getData(path, false, null));
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void setPathValue(String path, String value) {
        try {
            if (zk.exists(path, false) == null) {
                return;
            } else {
                zk.setData(path, value.getBytes(), -1);
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void saveLedger(int ledgerIndex) {
        try {
            if (zk.exists(rootPath, false) == null) {
                String cr = zk.create(rootPath, String.valueOf(ledgerIndex).getBytes(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                log.debug("CREATE NODE:" + cr);
            } else {
                zk.setData(rootPath, String.valueOf(ledgerIndex).getBytes(), -1);
            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void restart() {
        if (zk != null) {
            try {
                zk.close();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            zk = null;
        }
        if (zk == null) {
            synchronized (ZooKeeper.class) {
                if (zk == null) {
                    try {
                        zk = new ZooKeeper(zkCluster, 30000, new SessionWatcher());

                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {

                    }
                }
            }
        }
    }

    public static class SessionWatcher implements Watcher {
        @Override
        public void process(WatchedEvent watchedEvent) {
            log.info("Receive Event:" + watchedEvent);
            //session timeout
            if (watchedEvent.getState() == Event.KeeperState.Expired) {
                ZookeeperClient.restart();
            }
        }
    }
}
