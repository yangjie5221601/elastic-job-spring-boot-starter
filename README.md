# elastic-job-spring-boot-starter
1 elastic-job-spring-boot-starter简介
  Elastic-Job-Lite定位为轻量级无中心化解决方案，使用jar包的形式提供分布式任务的协调服务.这个是基于elasetic-job的spring-boot-starter.
  
2 elastic-job-spring-boot-starter模块介绍
  com.chuangxin.elastic.job.model:实体类动态添加任务实体
  com.chuangxin.elastic.job.util:工具类负责读取注解配置
  com.chuangxin.elastic.job.enums: 枚举
  com.chuangxin.elastic.job.config: config包
  com.chuangxin.elastic.job.annotations: 注解包。elasticjobconfig注解
  com.chuangxin.elastic.job.autoconfig: 
  
3 Quick Start
     3.1 添加maven依赖
            <dependency>
                <groupId>com.chuangxin</groupId>
                <artifactId>elastic-job-spring-boot-starter</artifactId>
                <version>1.0.0</version>
            </dependency>

      3.2 application.yml添加以下配置项
            elastic.job.zk.serverLists=localhost:2181 //多个zk逗号分割
            elastic.job.zk.namespace=order-wtable-job
	    elastic.job.enable=true// true启动false不启动

      3.3 定义任务
       @Log4j2
       @ElasticJobConf(name = "orderTotalCheckJob", cron = "0 0 1 * * ?", description = "订单总数 check job")
       public class OrderTotalCheckJob implements SimpleJob {
	@Autowired
	private OrderWtableService orderWtableService;
	@Autowired
	private PrestoTaskService prestoTaskService;
	/**
	 * 订单数量比较值
	 */
	private static final int ORDER_NUMBER = 10;


	@Override
	public void execute(ShardingContext shardingContext) {
		Long orderTotalPresto = prestoTaskService.getOrderTotal();
		log.info("presto order total:" + orderTotalPresto);
		Long orderDb = orderWtableService.getOrderCount();
		log.info("order in xinche order wtable database total:" + orderTotalPresto);
		long num = orderTotalPresto - orderDb;
		if (num > ORDER_NUMBER) {
			log.error("订单数量同步预警,相差{}条", num);
		}

	}
       }
      3.4 动态添加定时任务:
     1自定义job定时任务:
     public class TestJob implements SimpleJob {
	@Override
	public void execute(ShardingContext shardingContext) {
		System.out.println("ces -------------------");
	}
    }
    2 注入servicebean
    @Resource
    private DynamicElasticJobService dynamicElasticJobService;
    3 调用add方法添加定时任务
    ElasticJob job=new ElasticJob("SimpleJob","com.guazi.xinche.wtable.service.biz.job.TestJob","测试","0/5 * * * * ?");
    dynamicElasticJobService.addElasticJob(job);     
 4 elastic-job-spring-boot-starter更新日志:
   1.0.1:
   1.0.2:支持动态添加定时任务
 5 联系我们
     微信:Y704593386
