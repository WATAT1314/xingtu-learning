#!/usr/bin/env bash
# 一键演示核心链路（服务需已在 8081 启动）
set -e
BASE=${BASE:-http://localhost:8081}
H='Content-Type: application/json'

echo "== 1. 我的课表"
curl -s "$BASE/api/learning/lessons?userId=1"; echo

echo "== 2. 上报学习进度（30 秒，未学完）"
curl -s -X POST "$BASE/api/learning/records" -H "$H" \
  -d '{"userId":1,"lessonId":1,"sectionId":101,"sectionType":1,"moment":30,"finished":false}'; echo

echo "== 3. 同一小节再报（120 秒，学完）—— Redis 中会被合并成一条"
curl -s -X POST "$BASE/api/learning/records" -H "$H" \
  -d '{"userId":1,"lessonId":1,"sectionId":101,"sectionType":1,"moment":120,"finished":true}'; echo

echo "== 4. 查看合并缓冲区"
curl -s "$BASE/api/learning/pending?lessonId=1"; echo

echo "== 5. 等 6 秒后查看落库结果"
sleep 6
curl -s "$BASE/api/learning/records?userId=1&lessonId=1"; echo
curl -s "$BASE/api/learning/summary?userId=1"; echo

echo "== 6. 点赞 / 重复点赞 / 取消"
curl -s -X POST "$BASE/api/like" -H "$H" -d '{"userId":2,"bizType":1,"bizId":1001}'; echo
curl -s -X POST "$BASE/api/like" -H "$H" -d '{"userId":3,"bizType":1,"bizId":1001}'; echo
curl -s -X DELETE "$BASE/api/like" -H "$H" -d '{"userId":3,"bizType":1,"bizId":1001}'; echo

echo "== 7. 签到与排行榜"
curl -s -X POST "$BASE/api/points/sign?userId=1"; echo
curl -s "$BASE/api/points/rank?topN=5"; echo

echo "== 8. 领券（Lua 原子扣减）与重复领取拦截"
for id in 1 2 3 4; do curl -s -X POST "$BASE/api/coupon/$id/receive?userId=1"; echo; done
curl -s -X POST "$BASE/api/coupon/1/receive?userId=1"; echo

echo "== 9. 计算最优优惠组合（订单 260 元）"
curl -s -X POST "$BASE/api/coupon/best" -H "$H" -d '{"userId":1,"orderAmount":260}'; echo
