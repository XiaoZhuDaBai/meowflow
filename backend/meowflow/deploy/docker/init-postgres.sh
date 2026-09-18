#!/bin/bash
# PostgreSQL 初始化脚本
# 在 docker-entrypoint-initdb.d 中运行，创建必要的数据库和用户

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- 创建扩展
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    CREATE EXTENSION IF NOT EXISTS "pg_trgm";
    
    -- 确保数据库使用 UTF-8
    ALTER DATABASE meowflow SET client_encoding = 'UTF8';
    
    -- 授予权限
    GRANT ALL PRIVILEGES ON DATABASE meowflow TO meowflow;
EOSQL

echo "PostgreSQL initialization completed"
