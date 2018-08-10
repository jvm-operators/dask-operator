from dask_kubernetes import KubeCluster

cluster = KubeCluster.from_yaml('/worker-spec.yml')
cluster.scale_up(2)
