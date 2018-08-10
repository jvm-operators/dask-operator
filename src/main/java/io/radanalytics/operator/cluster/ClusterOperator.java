package io.radanalytics.operator.cluster;

import io.radanalytics.operator.common.AbstractOperator;
import io.radanalytics.operator.common.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.radanalytics.operator.common.AnsiColors.*;

@Operator(forKind = "cluster", prefix = "radanalytics.io", infoClass = ClusterInfo.class)
public class ClusterOperator extends AbstractOperator<ClusterInfo> {

    private static final Logger log = LoggerFactory.getLogger(AbstractOperator.class.getName());

    private final RunningClusters clusters;
    private KubernetesSparkClusterDeployer deployer;

    public ClusterOperator() {
        this.clusters = new RunningClusters();
    }

    protected void onAdd(ClusterInfo cluster) {
        ProcessRunner.runPythonScript("/start-cluster.py");

//        Properties props = new Properties();
//        props.put("python.home","/usr/local/lib/python2.7/site-packages");
//        props.put("python.console.encoding", "UTF-8"); // Used to prevent: console: Failed to install '': java.nio.charset.UnsupportedCharsetException: cp0.
//        props.put("python.security.respectJavaAccessibility", "false"); //don't respect java accessibility, so that we can access protected members on subclasses
//        props.put("python.import.site","false");
//
//        Properties preprops = System.getProperties();
//
//        PythonInterpreter.initialize(preprops, props, new String[0]);
//
//        PythonInterpreter interpreter = new PythonInterpreter();
//        interpreter.exec("import sys; sys.path.append('/usr/local/lib/python2.7/site-packages')\n" +
//                "from dask_kubernetes import KubeCluster\n" +
//                "cluster = KubeCluster.from_yaml('/worker-spec.yml')\n" +
//                "cluster.scale_up(2)\n");

//        KubernetesResourceList list = getDeployer().getResourceList(cluster);
//        client.resourceList(list).createOrReplace();
//        clusters.put(cluster);
    }

    protected void onDelete(ClusterInfo cluster) {
        String name = cluster.getName();
        client.services().withLabels(getDeployer().getDefaultLabels(name)).delete();
        client.replicationControllers().withLabels(getDeployer().getDefaultLabels(name)).delete();
        client.pods().withLabels(getDeployer().getDefaultLabels(name)).delete();
        clusters.delete(name);
    }

    protected void onModify(ClusterInfo newCluster) {
        String name = newCluster.getName();
        String newImage = newCluster.getCustomImage();
        int newMasters = newCluster.getMasterNodes();
        int newWorkers = newCluster.getWorkerNodes();
        ClusterInfo existingCluster = clusters.getCluster(name);
        if (null == existingCluster) {
            log.error("something went wrong, unable to scale existing cluster. Perhaps it wasn't deployed properly.");
            return;
        }

        if (existingCluster.getWorkerNodes() != newWorkers) {
            log.info("{}scaling{} from {}{}{} worker replicas to {}{}{}", ANSI_G, ANSI_RESET, ANSI_Y,
                    existingCluster.getWorkerNodes(), ANSI_RESET, ANSI_Y, newWorkers, ANSI_RESET);
            client.replicationControllers().withName(name + "-w").scale(newWorkers);
            clusters.put(newCluster);
        }
        // todo: image change, masters # change for k8s
    }

    public KubernetesSparkClusterDeployer getDeployer() {
        if (this.deployer == null) {
            this.deployer = new KubernetesSparkClusterDeployer(client, entityName, prefix);
        }
        return deployer;
    }
}
