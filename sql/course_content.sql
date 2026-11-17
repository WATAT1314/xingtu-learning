-- 星途学堂课程内容（自动生成）
USE xingtu_learning;

DELETE FROM section;
DELETE FROM course;

INSERT INTO course(id, title, cover, summary, level, duration_h, tags) VALUES (1001, '星途 · Java 微服务架构实战', '🏛️', '从单体到微服务的完整演进路径：服务拆分、注册发现、配置中心、网关、容错与链路追踪。每章都配可运行的关键代码片段。', '进阶', 12, 'Java,Spring Cloud,微服务,架构');
INSERT INTO course(id, title, cover, summary, level, duration_h, tags) VALUES (1002, '星途 · Redis 高并发实战', '⚡', '围绕真实高并发场景讲透 Redis：数据结构选型、缓存三大问题、分布式锁、延迟队列、排行榜与 Lua 原子脚本。', '进阶', 8, 'Redis,缓存,高并发,Lua');

INSERT INTO section(course_id, idx, title, minutes, content, quiz) VALUES (1001, 1, '为什么需要微服务：单体架构的三道坎', 25, '## 单体架构的三道坎

**第一坎：编译部署耦合**
一个 30 万行的单体工程，改一行代码重新构建要 8 分钟，发布必须整体停机。团队里 20 个人排队发版。

**第二坎：资源无法按需分配**
订单模块占 70% 流量，商品浏览只占 10%，但单体只能整体扩容 10 份。等于为订单多买了 9 份用不上的商品服务。

**第三坎：故障会全站传染**
一次内存泄漏，整个应用 OOM，所有业务一起挂。无法做故障隔离。

## 微服务怎么解

| 问题 | 微服务的解法 |
| --- | --- |
| 编译部署耦合 | 按业务域拆成独立服务，各自独立构建发布 |
| 资源浪费 | 热点服务单独扩容 |
| 故障传染 | 服务间进程隔离，配合熔断降级 |

## 拆分的正确姿势

按 **业务能力** 拆，不按技术分层拆。

- ✅ 订单服务、商品服务、支付服务
- ❌ 控制层服务、业务层服务、数据层服务

> 判断标准：一个服务应该能独立完成一件业务闭环，且能被一个 5-8 人小组完整负责（康威定律）。
', '{"q": "以下哪种拆分方式符合微服务的业务域拆分原则？", "options": ["按 Controller / Service / DAO 分层拆分", "按订单、商品、支付等业务能力拆分", "按数据库表拆分为每表一个服务", "按开发人员数量平分为 20 个服务"], "answer": 1}');
INSERT INTO section(course_id, idx, title, minutes, content, quiz) VALUES (1001, 2, '服务拆分实战：从一张订单看边界划分', 30, '## 以订单为例的边界划分

一个下单动作，横跨多个业务域。我们沿订单流程走一遍：

```
用户下单 → 校验商品 → 扣减库存 → 计算优惠 → 生成订单 → 发起支付
```

**错误做法**：全部塞进订单服务，订单服务里连商品表、库存表、优惠券表。结果订单服务与商品服务数据库互相访问，彻底耦合。

**正确做法**：按「数据归属」划边界。

| 数据 | 归属服务 | 订单服务如何拿到 |
| --- | --- | --- |
| 商品信息、价格 | 商品服务 | RPC 调用查询 |
| 库存数量 | 库存服务 | RPC 调用扣减 |
| 优惠券规则 | 营销服务 | RPC 调用计算 |
| 订单主体、订单明细 | 订单服务 | 自有数据库 |

## 边界的三个检验问题

1. **这个表的数据，谁有写权限？** 只能有一个服务写，其他服务只读接口。
2. **服务挂了，业务还能转吗？** 库存服务挂，下单要能降级（比如先下单后扣库存）。
3. **改动需求时，要动几个服务？** 只动一个说明边界清晰。

## 反模式：共享数据库

```
❌ 订单服务 ──直连──→ 商品库
❌ 商品服务 ──直连──→ 订单库
```

共享数据库是最常见的微服务伪拆分。表结构一改，所有服务全部要改，等于没拆。
', '{"q": "微服务拆分中最应该避免的反模式是？", "options": ["服务之间通过 HTTP 调用", "多个服务共享同一个数据库并直接跨库读写", "每个服务有独立的数据库", "服务按业务能力命名"], "answer": 1}');
INSERT INTO section(course_id, idx, title, minutes, content, quiz) VALUES (1001, 3, '服务注册与发现：Nacos 是怎么工作的', 35, '## 没有注册中心时的困境

商品服务要调用库存服务，库存服务部署了 5 个实例，IP 随扩缩容变化。如果写死在配置文件里：

```yaml
inventory:
  hosts:
    - 10.0.1.11:8081
    - 10.0.1.12:8081
```

扩容一个实例 → 改配置 → 重启所有调用方。运维噩梦。

## 注册中心接管这件事

```
启动时：库存服务 → 注册自己（IP:PORT + 元数据）到 Nacos
调用时：商品服务 → 问 Nacos「库存服务有哪些实例」→ 拿列表 → 负载均衡选一个
```

**Nacos 维持实例列表的新鲜度，靠心跳**：

- 客户端每 5 秒发一次心跳
- 15 秒没心跳 → 标记不健康
- 30 秒没心跳 → 从列表摘除

## 临时实例 vs 持久实例

| 类型 | 健康检查 | 典型场景 |
| --- | --- | --- |
| 临时实例 | 客户端主动上报心跳 | 微服务（默认） |
| 持久实例 | 服务端主动探测 | 数据库、中间件 |

## 关键代码

```java
// 服务注册（Spring Cloud Alibaba）
@EnableDiscoveryClient
@SpringBootApplication
public class InventoryApplication { }

// 服务调用（Feign 声明式）
@FeignClient(name = "inventory-service")
public interface InventoryClient {
    @PostMapping("/inventory/deduct")
    Result<Void> deduct(@RequestParam Long skuId, @RequestParam int count);
}
```

调用方只写服务名 `inventory-service`，具体 IP 由 Nacos + 负载均衡决定。
', '{"q": "Nacos 中临时实例在多少秒无心跳后会被从服务列表摘除？", "options": ["5 秒", "15 秒", "30 秒", "60 秒"], "answer": 2}');
INSERT INTO section(course_id, idx, title, minutes, content, quiz) VALUES (1001, 4, '配置中心：一套代码跑多环境', 30, '## 配置散落的问题

开发、测试、生产三套环境，数据库地址、Redis 密码、第三方密钥各不相同。传统做法：

```
application-dev.yml
application-test.yml
application-prod.yml
```

问题：改一个生产配置要走代码评审、打包、发布全流程。紧急改个开关等半小时。

## 配置中心的三层结构

Nacos 配置按 `namespace → group → dataId` 组织：

```
namespace: prod          # 环境隔离
  group: ORDER-SERVICE   # 服务分组
    dataId: order-service.yaml   # 具体配置文件
```

## 动态刷新

```java
@RestController
@RefreshScope   // 配置变更时重新注入
public class OrderController {

    @Value("${order.max-items:20}")
    private int maxItems;   // 改了 Nacos 配置，无需重启立即生效
}
```

## 配置的三条实践

1. **敏感信息不放配置中心明文**：密码走密钥管理服务，配置中心只存引用
2. **必须有版本与回滚**：改错了能一键回到上一版
3. **本地要有兜底**：配置中心挂掉时用本地默认值启动，不能全站起不来
', '{"q": "让配置变更无需重启立即生效，Spring Cloud 中需要加哪个注解？", "options": ["@Configuration", "@RefreshScope", "@EnableConfig", "@DynamicValue"], "answer": 1}');
INSERT INTO section(course_id, idx, title, minutes, content, quiz) VALUES (1001, 5, 'API 网关：所有流量的统一入口', 40, '## 为什么需要网关

微服务拆开后，前端要记住 20 个服务的地址。而且鉴权、限流、日志这些横切逻辑，每个服务都要写一遍。

网关把它们收拢到一个地方：

```
                ┌──────────────┐
   客户端 ─────→│  API 网关     │
                └──────┬───────┘
         ┌─────────────┼─────────────┐
         ▼             ▼             ▼
     订单服务      商品服务      库存服务
```

## 网关的五大职责

| 职责 | 说明 |
| --- | --- |
| 路由转发 | `/api/order/**` → 订单服务 |
| 统一鉴权 | 在入口校验 JWT，下游服务不再重复校验 |
| 限流熔断 | 单 IP 每秒 100 次，超了直接返回 429 |
| 协议转换 | 外部 HTTP，内部 gRPC |
| 日志埋点 | 统一记录链路 ID，方便追踪 |

## Spring Cloud Gateway 路由配置

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/order/**
          filters:
            - StripPrefix=2
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 100
```

`lb://` 表示走服务发现 + 负载均衡，不用写死 IP。

## 网关的坑

**网关不要写业务逻辑**。曾见过在网关里查数据库做用户画像，结果网关成了最大瓶颈，一挂全站不可用。网关只做「转发 + 横切」。
', '{"q": "网关配置中 uri 写成 lb://order-service 的含义是？", "options": ["直接连接本地 8080 端口", "通过服务发现获取实例并负载均衡", "使用 load balancer 硬件设备", "走长连接协议"], "answer": 1}');
INSERT INTO section(course_id, idx, title, minutes, content, quiz) VALUES (1001, 6, '服务容错：熔断、降级、隔离', 35, '## 雪崩是怎么发生的

```
订单服务等待商品服务（3 秒超时）
→ 线程池被占满
→ 订单服务不可用
→ 依赖订单服务的其他服务也卡住
→ 全站雪崩
```

一个下游慢，能拖垮整条链路。

## 三道防线

**1. 超时**：必须设。默认不设超时是事故之源。

```java
@FeignClient(name = "product-service", configuration = FeignTimeoutConfig.class)
```

**2. 熔断**：连续失败率达阈值，直接短路，不再调用下游，快速失败。

```
CLOSED（正常） → 失败率 > 50% → OPEN（熔断，直接返回兜底）
                                    ↓ 等待 10 秒
                                HALF-OPEN（放少量请求试探）
                                    ↓ 成功
                                  CLOSED
```

**3. 降级**：熔断后返回什么？不能返回 500，要给用户可接受的兜底。

```java
@GetMapping("/recommend")
public Result<List<Product>> recommend() {
    try {
        return Result.ok(productClient.hotList());
    } catch (Exception e) {
        // 降级：返回本地缓存的兜底推荐
        return Result.ok(cacheService.getFallbackList());
    }
}
```

## 隔离：别让一个下游占满线程池

按下游服务划分独立线程池。商品服务慢，只占满商品线程池，不影响支付服务调用。
', '{"q": "熔断器处于 OPEN 状态后，下一步通常会进入哪个状态？", "options": ["直接回到 CLOSED", "进入 HALF-OPEN 放少量请求试探", "永久保持 OPEN", "进入 DISABLED"], "answer": 1}');
INSERT INTO section(course_id, idx, title, minutes, content, quiz) VALUES (1001, 7, '分布式事务：订单和库存怎么保证一致', 40, '## 问题的本质

```
订单服务：INSERT order  ✅
库存服务：UPDATE stock  ❌ 网络超时
```

订单建了，库存没扣。数据不一致。

## 方案一：TCC（Try-Confirm-Cancel）

业务层面手动实现三阶段：

```
Try     ：冻结库存（stock_frozen += 1，stock_available -= 1）
Confirm ：真正扣减（stock_frozen -= 1）
Cancel  ：释放冻结（stock_frozen -= 1，stock_available += 1）
```

优点：强一致、性能好。缺点：每个业务要写三个方法，侵入性强。

## 方案二：本地消息表（最终一致）

```sql
-- 订单库
BEGIN;
INSERT INTO orders(...) VALUES(...);
INSERT INTO local_message(id, topic, payload, status) VALUES(..., ''PENDING'');
COMMIT;

-- 定时任务扫描 PENDING，投递到 MQ，成功后改 status=''SENT''
```

库存服务消费消息扣库存，做好幂等（用消息 ID 去重）。

## 方案三：Saga（长事务编排）

把大事务拆成一串小事务，每步都有补偿动作：

```
创建订单 → 扣库存 → 扣优惠券 → 发起支付
    ↓失败      ↓失败      ↓失败
  取消订单 ← 回补库存 ← 退还优惠券
```

## 怎么选

| 场景 | 推荐 |
| --- | --- |
| 资金、强一致要求高 | TCC |
| 订单类，可接受秒级延迟 | 本地消息表 / MQ |
| 跨多服务长流程 | Saga |

**绝大多数业务选最终一致就够了**，别一上来就 TCC。
', '{"q": "本地消息表方案中，保证库存服务不重复扣减的关键是？", "options": ["给库存表加索引", "消费端用消息 ID 做幂等去重", "增加数据库连接数", "把事务隔离级别调到 SERIALIZABLE"], "answer": 1}');
INSERT INTO section(course_id, idx, title, minutes, content, quiz) VALUES (1001, 8, '链路追踪：一次请求经过了哪些服务', 30, '## 没有链路追踪时排查有多痛

用户说「下单很慢」。日志分散在 6 个服务的 12 台机器上，靠时间戳猜？根本串不起来。

## TraceID 贯穿全链路

```
网关生成 TraceID = a1b2c3
  ├─ 订单服务  [a1b2c3] 耗时 120ms
  │   ├─ 商品服务 [a1b2c3 / span-1] 耗时 30ms
  │   └─ 库存服务 [a1b2c3 / span-2] 耗时 850ms  ← 就是它慢
  └─ 返回
```

一次请求一个 TraceID，每个服务内部的调用是一个 Span。这样一眼看出瓶颈在库存服务。

## 实现要点

**1. TraceID 透传**
网关生成，通过 HTTP Header 透传，日志格式里带上：

```
2026-11-17 01:20:33 [a1b2c3] INFO  OrderService - 创建订单
```

**2. MDC 线程上下文**
```java
MDC.put("traceId", traceId);
try {
    // 业务逻辑，日志自动带 traceId
} finally {
    MDC.clear();
}
```

**3. 异步场景要手动传递**
线程池、MQ、定时任务里 MDC 会丢，要显式透传。

## 工具选型

| 工具 | 特点 |
| --- | --- |
| SkyWalking | 无侵入，Java agent 挂载，国内常用 |
| Zipkin | 轻量，Spring Cloud Sleuth 原生支持 |
| Jaeger | CNCF 项目，K8s 生态友好 |
', '{"q": "链路追踪中，标识「一次完整请求」的字段是？", "options": ["SpanID", "TraceID", "SessionID", "RequestID"], "answer": 1}');
INSERT INTO section(course_id, idx, title, minutes, content, quiz) VALUES (1001, 9, '服务监控：上线后怎么知道它好不好', 30, '## 四个黄金指标

Google SRE 提出的监控四信号：

| 指标 | 含义 | 例子 |
| --- | --- | --- |
| 延迟 Latency | 请求耗时分布 | P99 < 200ms |
| 流量 Traffic | QPS | 当前 3200 QPS |
| 错误 Errors | 失败率 | 5xx 占比 0.3% |
| 饱和度 Saturation | 资源水位 | CPU 78%，线程池 90% |

**只看平均值是陷阱**。平均 100ms 但 P99 是 8 秒，意味着 1% 的用户在骂娘。

## Spring Boot Actuator 暴露指标

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    tags:
      application: order-service
```

访问 `/actuator/prometheus` 就能被 Prometheus 抓取。

## 告警要克制

```yaml
# 好的告警：明确、可行动
- alert: 订单服务错误率过高
  expr: rate(http_server_errors_total[5m]) > 0.01
  for: 5m

# 坏的告警：CPU 80% 不代表有问题，半夜吵醒人
- alert: CPU高
  expr: cpu_usage > 0.8
```

告警三原则：**能行动、有上下文、别疲劳**。
', '{"q": "监控四个黄金指标中，用来发现「长尾慢请求」的是？", "options": ["平均延迟", "P99 延迟", "总请求数", "CPU 使用率"], "answer": 1}');
INSERT INTO section(course_id, idx, title, minutes, content, quiz) VALUES (1001, 10, '灰度发布：新版本怎么安全上线', 35, '## 直接全量发布的代价

新版本有 bug，一发布 100% 用户受影响。回滚要 10 分钟，期间损失真实订单。

## 灰度发布的四种形态

**1. 金丝雀（按比例）**
```
v1: 95% 流量
v2: 5%  流量   ← 观察 30 分钟，指标正常再扩大
```

**2. 按用户（A/B）**
```
内部员工 → v2
普通用户 → v1
```
适合先让内部用，或者对特定用户群测试。

**3. 按地域**
新版本先上某个城市，观察真实流量表现。

**4. 蓝绿部署**
```
蓝环境（当前生产）  ← 100% 流量
绿环境（新版本）    ← 部署好，随时切换
```
切换是改一下负载均衡指向，回滚也是秒级切回。

## 网关实现按比例灰度

```yaml
- id: order-v2
  uri: lb://order-service-v2
  predicates:
    - Path=/api/order/**
    - Weight=order-group, 5      # 5% 流量
- id: order-v1
  uri: lb://order-service-v1
  predicates:
    - Path=/api/order/**
    - Weight=order-group, 95     # 95% 流量
```

## 灰度的前提

- 数据库变更必须**向前兼容**（新旧版本都能读）
- 准备好回滚预案
- 有可对比的监控指标
', '{"q": "蓝绿部署与金丝雀发布的核心区别是？", "options": ["蓝绿是两套完整环境切换，金丝雀是按比例逐步放量", "蓝绿只能用于数据库，金丝雀只能用于应用", "两者完全相同", "金丝雀必须停机，蓝绿不用"], "answer": 0}');
INSERT INTO section(course_id, idx, title, minutes, content, quiz) VALUES (1001, 11, '微服务安全：认证与授权怎么做', 35, '## 认证 vs 授权

- **认证（Authentication）**：你是谁？→ 校验 Token
- **授权（Authorization）**：你能干什么？→ 校验权限

## 集中式鉴权的架构

```
客户端 ──[JWT]──→ 网关
                   │ 1. 验签 + 过期校验
                   │ 2. 解析出 userId / roles
                   │ 3. 塞进 Header 传给下游
                   ▼
              微服务（信任网关，不再验签）
```

好处：签名校验只做一次，下游服务轻量。

## JWT 的结构

```
eyJhbGciOiJIUzI1NiJ9 . eyJ1c2VySWQiOjF9 . signature
      header              payload         signature
```

**payload 是 Base64 编码，不是加密**。任何人都能解开看到内容，所以绝不能放密码等敏感信息。

## 三个安全要点

1. **密钥不能硬编码**，走配置中心加密或 KMS
2. **Token 要能主动失效**：登出、改密码后旧 Token 必须作废 → 用 Redis 存黑名单或版本号
3. **网关与下游要建立信任边界**：下游只监听内网，禁止外部直连

## 权限模型

```java
@PreAuthorize("hasRole(''ADMIN'') or #userId == authentication.principal.id")
public Result<Order> detail(Long userId, Long orderId) { }
```

简单的用 RBAC（角色），复杂的用 ABAC（属性），别过度设计。
', '{"q": "JWT 的 payload 部分是什么形式的？", "options": ["加密的密文，不可解读", "Base64 编码，可被解码查看", "二进制压缩数据", "哈希摘要"], "answer": 1}');
INSERT INTO section(course_id, idx, title, minutes, content, quiz) VALUES (1001, 12, '架构演进复盘：什么时候不该上微服务', 25, '## 微服务不是免费的

引入微服务，你同时引入了：

- 分布式事务
- 服务治理（注册、配置、网关）
- 链路追踪、监控告警
- 更复杂的测试与部署
- 运维成本成倍增加

**一个 5 人团队维护 3 个服务的成本，可能高于维护 1 个单体。**

## 不该上微服务的信号

| 信号 | 说明 |
| --- | --- |
| 团队 < 10 人 | 沟通成本小于拆分收益 |
| 业务还没跑通 | 边界天天变，拆了就得重拆 |
| 没有自动化运维 | 手工部署 20 个服务是灾难 |
| QPS < 1000 | 单体完全扛得住 |

## 演进路线建议

```
第 1 阶段：模块化单体
  一个应用，但代码按业务域分模块，模块间只调接口不跨模块查库

第 2 阶段：抽离独立服务
  把压力最大、变更最频繁的模块（如订单）先拆出去

第 3 阶段：服务治理完善
  补齐注册中心、配置中心、网关、链路追踪

第 4 阶段：全面微服务
  按业务域完整拆分
```

## 一句话总结

> 微服务解决的是**组织协作**与**独立伸缩**问题，不是性能问题。
> 如果你的痛点是性能，先加缓存、优化 SQL、加索引，别急着拆服务。
', '{"q": "以下哪种情况最不适合引入微服务？", "options": ["团队 200 人，业务边界清晰", "团队 6 人，业务模式还在探索期", "订单模块流量是其他模块 10 倍", "需要独立扩容热点服务"], "answer": 1}');
INSERT INTO section(course_id, idx, title, minutes, content, quiz) VALUES (1002, 1, 'Redis 为什么快：单线程的取舍', 25, '## 快在哪

Redis 单线程却能做到 10 万 QPS，靠三件事：

**1. 纯内存操作**
读内存约 100 纳秒，读磁盘（SSD）约 100 微秒。差 1000 倍。

**2. 单线程避免锁竞争**
多线程操作同一份数据要加锁，加锁就有上下文切换和等待。Redis 干脆单线程，没有竞争就没有锁。

**3. IO 多路复用**
一个线程用 epoll 同时监听上万个连接，谁有数据就处理谁，不阻塞等待。

```
单线程 + epoll：
  连接A有数据 → 处理A
  连接B有数据 → 处理B
  都没数据 → 阻塞在 epoll_wait，不占 CPU
```

## 单线程的代价

**一个慢命令阻塞所有人**。比如：

```
KEYS *              # 遍历所有 key，100 万 key 卡 5 秒
HGETALL big_hash    # 返回 10 万字段，网络传输卡住
DEL big_list        # 删除百万元素的 list
```

线上禁用 `KEYS`，用 `SCAN` 渐进遍历。

## Redis 6.0 之后的多线程

注意：**多线程只用在了网络读写**，命令执行仍是单线程。所以「单线程模型」这个说法在命令执行层面依然成立。

## 为什么不用多线程执行命令

一旦多线程，就要处理并发读写同一 key，要加锁，复杂度飙升，而收益有限——因为瓶颈通常在网络和内存带宽，不在 CPU。
', '{"q": "Redis 单线程模型下，以下哪个命令在线上应该禁用？", "options": ["GET", "HGET", "KEYS", "INCR"], "answer": 2}');
INSERT INTO section(course_id, idx, title, minutes, content, quiz) VALUES (1002, 2, '五种基础类型与选型陷阱', 30, '## 五类型速查

| 类型 | 结构 | 典型场景 |
| --- | --- | --- |
| String | key → value | 计数、缓存对象 JSON、分布式锁 |
| Hash | key → {field: value} | 对象的部分字段更新 |
| List | key → [a,b,c] | 队列、最新 N 条 |
| Set | key → {a,b,c} | 去重、交集并集 |
| ZSet | key → {member: score} | 排行榜、延迟队列 |

## 三个常见选型陷阱

**陷阱一：用 String 存对象，每次改一个字段都要整体反序列化**

```java
// ❌ 改昵称要把整个 user 对象读出来、改、写回
String json = redis.get("user:1");
User u = JSON.parse(json);
u.setNickname("新昵称");
redis.set("user:1", JSON.toJSONString(u));

// ✅ Hash 只改一个字段
redis.opsForHash().put("user:1", "nickname", "新昵称");
```

**陷阱二：用 List 做去重**
List 不去重，要判重得遍历。用 Set。

**陷阱三：用 ZSet 存大量无排序需求的数据**
ZSet 每个元素都维护 score 和跳表索引，内存比 Set 高 30%+。不需要排序就用 Set。

## 容量估算

100 万用户的昵称用 Hash 存：
- key 开销约 50 字节
- 每个 field + value 约 80 字节
- 合计约 130MB

如果用 String 存 JSON 对象（含 20 个字段），可能膨胀到 1GB+。

**选型前先算内存**。
', '{"q": "需要频繁更新对象的某一个字段时，最合适的类型是？", "options": ["String 存整个 JSON", "Hash", "List", "ZSet"], "answer": 1}');
INSERT INTO section(course_id, idx, title, minutes, content, quiz) VALUES (1002, 3, '缓存穿透、击穿、雪崩：三种问题三种解法', 40, '## 三个名字很容易混，先分清

| 问题 | 场景 | 表现 |
| --- | --- | --- |
| 穿透 | 查不存在的数据 | 每次都打到数据库 |
| 击穿 | 热点 key 突然过期 | 瞬间大量请求打库 |
| 雪崩 | 大量 key 同时过期 | 数据库被打垮 |

## 缓存穿透

**攻击场景**：恶意请求 `id = -1`，缓存里没有，数据库也查不到，每次都不缓存 → 数据库被刷。

**解法一：缓存空值**
```java
if (user == null) {
    redis.set(key, "NULL", 60, SECONDS);  // 短 TTL 缓存空结果
    return null;
}
```

**解法二：布隆过滤器**（推荐）
把所有存在的 id 放进布隆过滤器。请求先过过滤器，不存在直接返回。

```java
if (!bloomFilter.mightContain(id)) {
    return null;   // 一定不存在
}
```

布隆过滤器的特点：**说不存在一定不存在，说存在可能不存在**（有误判率，无漏判）。

## 缓存击穿

热点商品下架又上架，key 刚好过期，1 万个请求同时打库。

**解法一：互斥锁**（保证只有一个线程回源）
```java
String lock = redis.setIfAbsent("lock:" + key, "1", 10, SECONDS);
if (lock) {
    try { loadFromDb(); } finally { redis.delete("lock:" + key); }
} else {
    Thread.sleep(50);   // 等别人加载完
    return redis.get(key);
}
```

**解法二：逻辑过期**（不设 TTL，value 里存过期时间）
读到逻辑过期数据时，异步起一个线程刷新，当前请求先返回旧值。保证高可用，代价是短暂不一致。

## 缓存雪崩

**解法：TTL 加随机扰动**
```java
int ttl = 3600 + random.nextInt(600);   // 3600~4200 秒
redis.set(key, value, ttl, SECONDS);
```
让过期时间分散，不要同一时刻集体失效。

再加一层：Redis 集群 + 本地缓存（Caffeine）做二级缓存兜底。
', '{"q": "布隆过滤器的核心特性是？", "options": ["说不存在一定不存在，说存在可能误判", "能精确判断元素是否存在", "可以删除已添加的元素", "占用内存与元素数量无关"], "answer": 0}');
INSERT INTO section(course_id, idx, title, minutes, content, quiz) VALUES (1002, 4, '分布式锁：从 SETNX 到 Redisson', 40, '## 最朴素的分布式锁

```java
Boolean ok = redis.opsForValue().setIfAbsent("lock:order:1", "1");
if (Boolean.TRUE.equals(ok)) {
    try { doBusiness(); } finally { redis.delete("lock:order:1"); }
}
```

**四个致命缺陷**：

**缺陷一：忘了设过期时间**
服务宕机，锁永远不释放，业务死锁。

```java
// 修复：原子设置值 + 过期时间
redis.opsForValue().setIfAbsent(key, "1", 30, TimeUnit.SECONDS);
```

**缺陷二：误删别人的锁**
线程 A 执行超过 30 秒，锁自动过期；线程 B 拿到锁；A 执行完把 B 的锁删了。

```java
// 修复：value 存唯一标识，删除前校验
String token = UUID.randomUUID().toString();
redis.opsForValue().setIfAbsent(key, token, 30, SECONDS);
// 删除时用 Lua 保证「校验 + 删除」原子
```

```lua
if redis.call(''GET'', KEYS[1]) == ARGV[1] then
    return redis.call(''DEL'', KEYS[1])
else
    return 0
end
```

**缺陷三：业务没执行完锁就过期了**
A 还在跑，锁过期，B 进来了。两个线程同时操作同一资源。

**缺陷四：主从切换丢锁**
主节点写入锁，还没同步到从节点就宕机，从节点升主，锁丢了。

## Redisson 怎么解决

**看门狗（Watch Dog）**：拿到锁后，后台线程每 10 秒（租期 1/3）自动续期，业务没结束锁就不会过期。

```java
RLock lock = redisson.getLock("lock:order:1");
try {
    lock.lock();          // 默认 30 秒租期，看门狗自动续
    doBusiness();
} finally {
    lock.unlock();
}
```

**RedLock 算法**：向 5 个独立 Redis 节点加锁，超过半数成功才算拿到锁。解决主从丢锁问题。但实现复杂且有争议，多数业务用单节点 Redisson 就够。
', '{"q": "Redisson 的「看门狗」机制主要解决什么问题？", "options": ["防止锁被其他线程误删", "业务未执行完时自动续期，避免锁提前过期", "提升加锁速度", "支持跨机房部署"], "answer": 1}');
INSERT INTO section(course_id, idx, title, minutes, content, quiz) VALUES (1002, 5, '延迟队列：用 ZSet 做定时任务', 35, '## 场景

- 订单 30 分钟未支付自动取消
- 下单 15 分钟后推送催付消息
- 优惠券到期前 1 天提醒

这些都是「在某个时间点执行某事」。

## ZSet 方案：score 存执行时间戳

```java
// 投递延迟任务
public void delay(String taskId, long delaySeconds) {
    long executeAt = System.currentTimeMillis() / 1000 + delaySeconds;
    redis.opsForZSet().add("xt:delay:queue", taskId, executeAt);
}

// 消费者轮询
@Scheduled(fixedDelay = 1000)
public void poll() {
    long now = System.currentTimeMillis() / 1000;
    Set<String> due = redis.opsForZSet()
            .rangeByScore("xt:delay:queue", 0, now, 0, 100);

    for (String taskId : due) {
        // 先移除再执行，避免重复消费
        Long removed = redis.opsForZSet().remove("xt:delay:queue", taskId);
        if (removed != null && removed > 0) {
            execute(taskId);
        }
    }
}
```

## 为什么用 `remove` 的返回值判断

多个消费者实例同时轮询，`remove` 返回 1 表示「我删成功了」，只有删成功的那个才执行，天然避免重复消费。

## 精度与吞吐

| 参数 | 影响 |
| --- | --- |
| 轮询间隔 1 秒 | 延迟精度约 1 秒 |
| 每次取 100 条 | 吞吐上限 |
| 轮询间隔调小 | 精度提高，CPU 占用上升 |

## 对比 MQ 延迟消息

| 方案 | 精度 | 可靠性 | 复杂度 |
| --- | --- | --- | --- |
| Redis ZSet | 秒级 | 中（重启可能丢） | 低 |
| RocketMQ 延迟消息 | 秒级 | 高 | 中 |
| RabbitMQ 死信队列 | 秒级 | 高 | 中 |

**要求不高、已有 Redis 的项目，ZSet 方案够用且最简单。**
', '{"q": "ZSet 延迟队列中，多个消费者同时轮询时避免重复消费的关键是？", "options": ["给 ZSet 加分布式锁", "用 remove 操作的返回值判断是否抢到任务", "每个消费者分配固定 key", "降低轮询频率"], "answer": 1}');
INSERT INTO section(course_id, idx, title, minutes, content, quiz) VALUES (1002, 6, '排行榜与附近的人：ZSet 进阶', 30, '## 排行榜

```java
// 加分
redis.opsForZSet().incrementScore("xt:rank:points", "user:1", 10);

// 取前 10 名（含分数）
Set<TypedTuple<String>> top = redis.opsForZSet()
        .reverseRangeWithScores("xt:rank:points", 0, 9);

// 查我的排名（从 0 开始，需 +1）
Long rank = redis.opsForZSet().reverseRank("xt:rank:points", "user:1");
```

复杂度：`ZINCRBY` O(log N)，`ZREVRANGE` O(log N + M)。百万人排行榜毫秒级。

## 同分怎么排

ZSet 按 score 排序，score 相同时按 member 字典序。想按「先达到者优先」，可以把分数做成：

```
score = 积分 * 10^10 + (MAX_TIMESTAMP - 达成时间)
```

积分相同时，时间早的分数更高。

## 「附近的人」

原始做法是计算两点距离，但要对全表做距离计算，O(N)。

**Redis GEO 用 GeoHash 把二维坐标编码成一维整数**，附近的人变成「查找编码相近的点」：

```java
// 写入位置
redis.opsForGeo().add("xt:geo:shops", new Point(116.404, 39.915), "shop:1");

// 查 3 公里内的店铺
GeoResults<RedisGeoCommands.GeoLocation<String>> results = redis.opsForGeo()
        .radius("xt:geo:shops", "user:pos", 3, Metrics.KILOMETERS);
```

底层还是 ZSet，score 是 GeoHash 值。所以 GEO 命令本质是 ZSet 的语法糖。

## 注意

GEO 精度约 0.6 米，够用。但如果要做「沿路线搜索」，还是得上专业的空间索引（如 Elasticsearch geo_shape）。
', '{"q": "Redis GEO 底层使用的数据结构是？", "options": ["List", "Hash", "ZSet", "String"], "answer": 2}');
INSERT INTO section(course_id, idx, title, minutes, content, quiz) VALUES (1002, 7, 'Lua 脚本：把多个命令变成原子操作', 35, '## 为什么需要 Lua

秒杀场景：先查库存，再扣减。

```java
int stock = Integer.parseInt(redis.get("stock"));
if (stock > 0) {
    redis.decr("stock");   // ← 这两步之间可能被其他线程插入
}
```

并发下，100 个线程同时读到 stock=1，全部通过判断，库存被扣成 -99。

## Lua 保证原子性

Redis 执行 Lua 脚本时是单线程、不被打断的。整个脚本要么全执行完，要么不执行。

```lua
local stock = tonumber(redis.call(''GET'', KEYS[1]) or ''0'')
if stock <= 0 then
    return 0            -- 已售罄
end
redis.call(''DECR'', KEYS[1])
return 1                -- 扣减成功
```

```java
DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA, Long.class);
Long result = redis.execute(script, List.of("stock:sku1"));
```

## 更贴近真实的秒杀脚本

```lua
-- KEYS[1]=库存key  KEYS[2]=已购用户set  ARGV[1]=userId
if redis.call(''SISMEMBER'', KEYS[2], ARGV[1]) == 1 then
    return -1                       -- 重复购买
end
local stock = tonumber(redis.call(''GET'', KEYS[1]) or ''0'')
if stock <= 0 then
    return 0                        -- 已售罄
end
redis.call(''DECR'', KEYS[1])
redis.call(''SADD'', KEYS[2], ARGV[1])
return 1                            -- 成功
```

一次网络往返完成「判重 + 判库存 + 扣减 + 记录」，原子且高效。

## 使用注意

1. **脚本要短**：Lua 执行期间阻塞所有其他命令
2. **不要写死 key**：key 必须通过 KEYS 传入，否则集群模式下路由错误
3. **避免循环大量数据**：脚本里遍历 10 万个 key 会让 Redis 卡住
4. **调试用 `SCRIPT LOAD` + `EVALSHA`**：避免每次传输脚本内容
', '{"q": "Lua 脚本中，key 必须通过 KEYS 数组传入而不是直接写死，主要原因是？", "options": ["写死会导致语法错误", "集群模式下需要据此路由到正确节点", "可以提升执行速度", "便于日志记录"], "answer": 1}');
INSERT INTO section(course_id, idx, title, minutes, content, quiz) VALUES (1002, 8, '持久化与高可用：RDB、AOF 与集群', 40, '## RDB：快照

某一时刻把内存数据全量 dump 到磁盘。

```
配置：save 900 1      # 900 秒内至少 1 次修改就触发
      save 300 10
      save 60 10000
```

**优点**：文件紧凑，恢复快，适合备份。
**缺点**：两次快照之间的数据会丢（最多丢 15 分钟）。

## AOF：日志重放

把每条写命令追加到文件。

```
配置：appendonly yes
      appendfsync everysec    # 每秒刷盘，最多丢 1 秒数据
```

**优点**：数据更安全。
**缺点**：文件体积大，恢复慢（要重放所有命令）。

## 生产推荐：混合持久化

Redis 4.0+ 支持 RDB + AOF 混合：AOF 文件前半部分是 RDB 全量快照，后半部分是增量命令。

```yaml
appendonly yes
aof-use-rdb-preamble yes    # 混合模式，兼顾恢复速度与数据安全
```

## 主从 + 哨兵 + 集群

**主从复制**：主写从读，分摊读压力。但从节点只是备份。

**哨兵（Sentinel）**：监控主节点，主挂了自动把一个从节点升为主。解决「自动故障转移」。

```
Sentinel 集群（3 个节点，避免误判）
  ├─ 监控 master
  ├─ master 挂了 → 投票选举新 master
  └─ 通知客户端新地址
```

**Cluster 集群**：数据分片。16384 个哈希槽分配到多个主节点。

```
节点A: slot 0-5460
节点B: slot 5461-10922
节点C: slot 10923-16383
```

key 通过 `CRC16(key) % 16384` 决定落在哪个槽。

**注意**：集群模式下，多 key 操作必须在同一个槽（用 hash tag `{user1}:name` 让相关 key 落同槽）。

## 三个方案怎么选

| 需求 | 方案 |
| --- | --- |
| 只是读多写少，要备份 | 主从 |
| 要自动故障转移，数据量 < 20GB | 哨兵 |
| 数据量大，要水平扩容 | Cluster |
', '{"q": "Redis Cluster 中，一个 key 归属于哪个槽由什么决定？", "options": ["随机分配", "CRC16(key) % 16384", "按 key 长度取模", "由客户端指定"], "answer": 1}');
