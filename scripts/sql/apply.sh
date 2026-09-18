#!/usr/bin/env bash
# 喵流 MeowFlow - 数据库一键初始化脚本
# 用法: ./apply.sh [PGUSER] [PGHOST] [PGPORT]
# 默认: PGUSER=postgres PGHOST=localhost PGPORT=5432

set -e

PGUSER="${1:-postgres}"
PGHOST="${2:-localhost}"
PGPORT="${3:-5432}"
DBNAME="meowflow"

export PGUSER PGHOST PGPORT

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "==> 初始化数据库 $DBNAME @ $PGHOST:$PGPORT (user=$PGUSER)"

echo "--> 00-init.sql"
psql -d postgres -f 00-init.sql

echo "--> 01-schema.sql"
psql -d "$DBNAME" -f 01-schema.sql

echo "--> 02-index.sql"
psql -d "$DBNAME" -f 02-index.sql

echo "--> 03-data.sql"
psql -d "$DBNAME" -f 03-data.sql

echo "✅ 数据库 $DBNAME 初始化完成"
echo "   默认账号: admin / meow@2026"
