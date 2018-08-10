from dask_kubernetes import KubeCluster
# import dask.config
#import dask.distributed

#dask.config.set({'kubernetes.name': 'myproject'})
cluster = KubeCluster.from_yaml('/home/jkremser/workspace/jvm-operators/dusk-operator/worker-spec.yml')
cluster.scale_up(2)
