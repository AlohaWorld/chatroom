# 简化聊天室（Java 21 + Netty 4.1.x）

这是一个用于课堂演示的“简化聊天室”原型，包含 server / client-cli / client-fx / tools / protocol 多模块 Maven 工程。
重点用于对比 **naive** 与 **backpressure** 两种并发/背压策略下的指标变化与“翻车现象”。

## 关键特性
- TCP 长连接 + 自定义协议（Length-Field + JSON）
- 登录、聊天、广播、心跳、退出
- 结构化日志（traceId/userId/channelId/msgType）
- 指标 HTTP 端点 `/metrics` / `/stats`
- 两种模式：`naive` 与 `backpressure`
- 压测工具 `loadgen`、故障注入工具 `fault-inject`

## 可靠性与语义
- 投递语义：best-effort（允许丢失，不保证必达）
- 顺序语义：同一连接内按接收顺序处理；广播顺序尽量保持，但受并发与丢弃策略影响
- 重复登录策略：**踢旧连接**（新登录成功，旧连接被关闭）
- 广播规则：**包含发送者**（便于端到端延迟统计）

## 协议摘要
- 帧格式：`length(4 bytes, big-endian)` + `payload(JSON)`
- payload
  - header: protocolVersion, msgType, msgId, traceId, ts, from, room
  - body: 随 msgType 变化（如 CHAT: text, sendTs）

## 构建
```bash
mvn -q -DskipTests package
```

## 一键脚本（推荐）
说明：所有脚本均使用 `jar` 方式启动。构建后即可运行。

macOS/Linux（sh）：
```bash
# Server (naive)
sh scripts/run-server.sh --mode naive

# CLI
sh scripts/run-cli.sh --host 127.0.0.1 --port 9000 --userId alice

# JavaFX
sh scripts/run-fx.sh

# LoadGen
sh scripts/run-loadgen.sh --clients 200 --msgRate 2000 --durationSec 30 --payloadSize 128

# FaultInject
sh scripts/run-fault-inject.sh --case all
```

Windows（bat）：
```bat
REM Server (naive)
scripts\\run-server.bat --mode naive

REM CLI
scripts\\run-cli.bat --host 127.0.0.1 --port 9000 --userId alice

REM JavaFX
scripts\\run-fx.bat

REM LoadGen
scripts\\run-loadgen.bat --clients 200 --msgRate 2000 --durationSec 30 --payloadSize 128

REM FaultInject
scripts\\run-fault-inject.bat --case all
```

## 运行：server（方式A：`java -jar`）
说明：构建后会生成可执行 jar `server/target/server-1.0-SNAPSHOT.jar`，运行依赖放在 `server/target/lib/`。

macOS/Linux：
```bash
# naive 模式
java -jar server/target/server-1.0-SNAPSHOT.jar \
  --port 9000 --mode naive --heartbeatIntervalSec 5 --heartbeatMissThreshold 3 --maxFrameBytes 1048576

# backpressure 模式（断开慢客户端阈值可调）
java -jar server/target/server-1.0-SNAPSHOT.jar \
  --port 9000 --mode backpressure --backpressureUnwritableThreshold 3
```

Windows（java.exe）：
```bat
REM naive 模式
java.exe -jar server\\target\\server-1.0-SNAPSHOT.jar ^
  --port 9000 --mode naive --heartbeatIntervalSec 5 --heartbeatMissThreshold 3 --maxFrameBytes 1048576

REM backpressure 模式
java.exe -jar server\\target\\server-1.0-SNAPSHOT.jar ^
  --port 9000 --mode backpressure --backpressureUnwritableThreshold 3
```

## 运行：server（方式B：`java -cp + 主类`）
macOS/Linux：
```bash
java -cp "server/target/classes:$(cat server/target/classpath.txt)" \
  com.example.chatroom.server.ServerMain \
  --port 9000 --mode naive --heartbeatIntervalSec 5 --heartbeatMissThreshold 3 --maxFrameBytes 1048576
```

Windows（java.exe）：
```bat
java.exe -cp "server\\target\\classes;@server\\target\\classpath.txt" ^
  com.example.chatroom.server.ServerMain ^
  --port 9000 --mode naive --heartbeatIntervalSec 5 --heartbeatMissThreshold 3 --maxFrameBytes 1048576
```

指标端点（默认端口=serverPort+1）：
```bash
curl http://127.0.0.1:9001/metrics
```

## 运行：CLI 客户端（方式A：`java -jar`）
说明：构建后会生成可执行 jar `client-cli/target/client-cli-1.0-SNAPSHOT.jar`，运行依赖放在 `client-cli/target/lib/`。

macOS/Linux：
```bash
java -jar client-cli/target/client-cli-1.0-SNAPSHOT.jar \
  --host 127.0.0.1 --port 9000 --userId alice
```

Windows（java.exe）：
```bat
java.exe -jar client-cli\\target\\client-cli-1.0-SNAPSHOT.jar ^
  --host 127.0.0.1 --port 9000 --userId alice
```

## 运行：CLI 客户端（方式B：`java -cp + 主类`）
macOS/Linux：
```bash
java -cp "client-cli/target/classes:$(cat client-cli/target/classpath.txt)" \
  com.example.chatroom.client.cli.CliMain \
  --host 127.0.0.1 --port 9000 --userId alice
```

Windows（java.exe）：
```bat
java.exe -cp "client-cli\\target\\classes;@client-cli\\target\\classpath.txt" ^
  com.example.chatroom.client.cli.CliMain ^
  --host 127.0.0.1 --port 9000 --userId alice
```

CLI 命令：
- `/login <userId>`
- `/send <text>`
- `/bye`

## 运行：JavaFX 客户端（方式A：`java -jar`）
说明：构建后会生成可执行 jar `client-fx/target/client-fx-1.0-SNAPSHOT.jar`，运行依赖放在 `client-fx/target/lib/`。

macOS/Linux：
```bash
java -jar client-fx/target/client-fx-1.0-SNAPSHOT.jar
```

Windows（java.exe）：
```bat
java.exe -jar client-fx\\target\\client-fx-1.0-SNAPSHOT.jar
```

## 运行：JavaFX 客户端（方式B：`java -cp + 主类`）
macOS/Linux：
```bash
java -cp "client-fx/target/classes:$(cat client-fx/target/classpath.txt)" \
  com.example.chatroom.client.fx.ChatFxApp
```

Windows（java.exe）：
```bat
java.exe -cp "client-fx\\target\\classes;@client-fx\\target\\classpath.txt" ^
  com.example.chatroom.client.fx.ChatFxApp
```

在 UI 中填写：Host/Port/User/MetricsPort，然后 Connect。

## 运行：loadgen（方式A：`java -jar`）
说明：`tools` 会生成两个可执行 jar：
- `tools/target/tools-1.0-SNAPSHOT.jar`（LoadGen）
- `tools/target/tools-1.0-SNAPSHOT-fault-inject.jar`（FaultInject）

macOS/Linux：
```bash
java -jar tools/target/tools-1.0-SNAPSHOT.jar \
  --host 127.0.0.1 --port 9000 --clients 200 --msgRate 2000 --durationSec 30 --payloadSize 128
```

Windows（java.exe）：
```bat
java.exe -jar tools\\target\\tools-1.0-SNAPSHOT.jar ^
  --host 127.0.0.1 --port 9000 --clients 200 --msgRate 2000 --durationSec 30 --payloadSize 128
```

## 运行：loadgen（方式B：`java -cp + 主类`）
macOS/Linux：
```bash
java -cp "tools/target/classes:$(cat tools/target/classpath.txt)" \
  com.example.chatroom.tools.LoadGenMain \
  --host 127.0.0.1 --port 9000 --clients 200 --msgRate 2000 --durationSec 30 --payloadSize 128
```

Windows（java.exe）：
```bat
java.exe -cp "tools\\target\\classes;@tools\\target\\classpath.txt" ^
  com.example.chatroom.tools.LoadGenMain ^
  --host 127.0.0.1 --port 9000 --clients 200 --msgRate 2000 --durationSec 30 --payloadSize 128
```

输出示例：
- `sent`：发送的 CHAT 数
- `recv`：接收到的 BROADCAST 数
- `p50/p95/p99`：端到端延迟（从 sendTs 到接收时刻）

## 运行：fault-inject（方式A：`java -jar`）
macOS/Linux：
```bash
# 全部注入
java -jar tools/target/tools-1.0-SNAPSHOT-fault-inject.jar \
  --host 127.0.0.1 --port 9000 --case all

# 指定单项
java -jar tools/target/tools-1.0-SNAPSHOT-fault-inject.jar \
  --host 127.0.0.1 --port 9000 --case unknown_type
```

Windows（java.exe）：
```bat
REM 全部注入
java.exe -jar tools\\target\\tools-1.0-SNAPSHOT-fault-inject.jar ^
  --host 127.0.0.1 --port 9000 --case all

REM 指定单项
java.exe -jar tools\\target\\tools-1.0-SNAPSHOT-fault-inject.jar ^
  --host 127.0.0.1 --port 9000 --case unknown_type
```

## 运行：fault-inject（方式B：`java -cp + 主类`）
macOS/Linux：
```bash
# 全部注入
java -cp "tools/target/classes:$(cat tools/target/classpath.txt)" \
  com.example.chatroom.tools.FaultInjectMain \
  --host 127.0.0.1 --port 9000 --case all

# 指定单项
java -cp "tools/target/classes:$(cat tools/target/classpath.txt)" \
  com.example.chatroom.tools.FaultInjectMain \
  --host 127.0.0.1 --port 9000 --case unknown_type
```

Windows（java.exe）：
```bat
REM 全部注入
java.exe -cp "tools\\target\\classes;@tools\\target\\classpath.txt" ^
  com.example.chatroom.tools.FaultInjectMain ^
  --host 127.0.0.1 --port 9000 --case all

REM 指定单项
java.exe -cp "tools\\target\\classes;@tools\\target\\classpath.txt" ^
  com.example.chatroom.tools.FaultInjectMain ^
  --host 127.0.0.1 --port 9000 --case unknown_type
```

支持的 case：
- `missing_fields`：缺失必要字段
- `unknown_type`：未知 msgType
- `bad_length`：伪造长度字段
- `too_large`：超大包
- `disconnect`：直接断开

## 复现“翻车现象”与对比实验

### A. naive 模式（翻车复现）
1) 启动 server：
```bash
java -jar server/target/server-1.0-SNAPSHOT.jar --port 9000 --mode naive
```
2) 运行高压 loadgen：
```bash
java -jar tools/target/tools-1.0-SNAPSHOT.jar \
  --host 127.0.0.1 --port 9000 --clients 300 --msgRate 4000 --durationSec 40 --payloadSize 256
```
3) 观察指标：
- `broadcast_latency_ms_p95` / `p99` 明显上升
- `unwritable_events_total` 增加（写缓冲水位被触发）
- `current_connections` 仍在，但 `recv` 可能下降或波动

### B. backpressure 模式（改进对比）
1) 启动 server：
```bash
java -jar server/target/server-1.0-SNAPSHOT.jar \
  --port 9000 --mode backpressure --backpressureUnwritableThreshold 3
```
2) 使用相同 loadgen 参数
3) 观察指标对比（至少 3 个）
- `broadcast_latency_ms_p95`/`p99` 不再失控
- `backpressure_drop_total` / `backpressure_disconnect_total` 有受控增长
- `disconnect_total{reason="backpressure"}` 可解释且系统不崩溃

## 关键配置参数
- `--port`：服务器端口
- `--mode`：`naive` / `backpressure`
- `--heartbeatIntervalSec`：心跳间隔（默认 5s）
- `--heartbeatMissThreshold`：连续丢心跳阈值（默认 3）
- `--maxFrameBytes`：最大帧大小（默认 1MB）
- `--backpressureUnwritableThreshold`：不可写连续次数阈值（默认 3）

## 说明
- 这是教学原型，重在观测与对比，并非生产级。
- 若环境性能不同，可调整 `clients/msgRate/payloadSize` 以更明显地观察“翻车”。
